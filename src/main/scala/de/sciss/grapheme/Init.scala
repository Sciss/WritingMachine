/*
 *  Init.scala
 *  (WritingMachine)
 *
 *  Copyright (c) 2011-2017 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.grapheme

import java.io.File

import de.sciss.lucre.stm
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.stm.TxnLike.peer
import de.sciss.lucre.synth.{Sys => SSys}
import de.sciss.nuages.{ControlPanel, NuagesView}
import de.sciss.synth
import de.sciss.synth.proc.Proc
import de.sciss.synth.{AudioBus, Server}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.stm.TMap
import scala.swing.Swing
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

object Init {
  import WritingMachine._

  private val identifier = "meta-diff"

  private val instanceRef = TMap.empty[Sys[_], Init[_]] // Ref.make[Init[S]]

  def instance[S <: Sys[S]](implicit tx: S#Tx): Init[S] = instanceRef(tx.system).asInstanceOf[Init[S]]

  def apply[S <: SSys[S]](r: NuagesView[S])(implicit tx: S#Tx): Init[S] = {
    require(!instanceRef.contains(tx.system))

    val panel = r.panel // .frame.panel
    val coll = ??? : Proc[S] // panel.collector.getOrElse(sys.error("Requires nuages to use collector"))
    val spat = DifferanceSpat(coll)
    val tv = if (tvUseTestFile) {
      Television.fromFile[S](new File(testDir, "euronews.aif"))
    } else {
      Television.live[S]()
    }
    import r.cursor
    val i = new Init[S](/* start, diff, */ spat, tv)
    instanceRef.put(tx.system, i) // set(i)

    tx.afterCommit {
      val s = Server.default
      startMeterSynth(
        r.controlPanel,
        AudioBus(s, s.config.outputBusChannels + tvChannelOffset, tvNumChannels),
        ??? // panel.masterBus.get
      )
      if (autoStart) Swing.onEDT {
        ??? // r.controlPanel.startClock()
      }
    }
    i
  }

  private def startMeterSynth(ctrl: ControlPanel, inBus: AudioBus, outBus: AudioBus): Unit = {
    import synth._
    import ugen._

    val s = inBus.server

    val df = SynthDef("post-master") {
      val inSig     = In.ar(inBus.index, inBus.numChannels)
      val outSig    = In.ar(outBus.index, outBus.numChannels)
      val sig       = Flatten(Seq(outSig, inSig))
      val meterTr   = Impulse.kr(20)
      val peak      = Peak.kr(sig, meterTr)
      val rms       = A2K.kr(Lag.ar(sig.squared, 0.1))
      val meterData = Zip(peak, rms) // XXX correct?
      SendReply.kr(meterTr, meterData, "/meters")
    }
    val syn = ??? : Synth // df.play(s, addAction = addToTail)
    val synID = syn.id
    ???
//    OSCResponder.add({
//      case Message("/meters", `synID`, 0, values@_*) =>
//        Swing.onEDT {
//          ctrl.meterUpdate(values.map(_.asInstanceOf[Float])(collection.breakOut))
//        }
//    }, s)
  }
}

final class Init[S <: Sys[S]] private(/* _phrase0: Phrase, val differance: DifferanceAlgorithm, */ val spat: DifferanceSpat[S],
                         val tv: Television[S])(implicit cursor: stm.Cursor[S]) {

  import GraphemeUtil._
  import Init._

  val numSectors: Int = WritingMachine.masterNumChannels

  var overlapMotion: Motion = Motion.exprand(1.1, numSectors - 1)

  var keepGoing = true

  private def act(): Unit = new Thread {
    override def run(): Unit = {
      val futP0 = cursor.atomic("meta-diff initial tv capture")(tx =>
        tv.capture(secondsToFrames(WritingMachine.initialPhraseFill))(tx))
      logNoTx(s"==== $identifier wait for initial tv capture ====")
      futP0.onComplete {
        case Failure(e) =>
          logNoTx(s"==== $identifier initial capture failed: ====")
          e.printStackTrace()
          Thread.sleep(1000)
        case Success(fP0) => actWithFile(fP0)
      }
    }

    start()
  }

  private def actWithFile(fP0: File): Unit = {
    val (differance, _p0) =
      cursor.atomic(s"$identifier : initialize algorithm") { implicit tx =>
        val p0      = Phrase.fromFile             [S](fP0)
        val db      = Database                    [S](databaseDir)
        val filler  = DifferanceDatabaseFiller    [S](db, tv)
        val thinner = DifferanceDatabaseThinner   [S](db)
        val trace   = PhraseTrace                 [S]()
        val query   = DifferanceDatabaseQuery     [S](db)
        val over    = DifferanceOverwriter        [S]()
        val overSel = DifferanceOverwriteSelector [S]()
        (DifferanceAlgorithm[S](/* spat, */ thinner, filler, trace, query, over, overSel, p0), p0)
      }

    var p = _p0
    val spatFuts = Array.fill(numSectors)(futureOf(()))
    var sector = 0
    while (keepGoing) {
      try {
        if (!spatFuts(sector).isCompleted) {
          logNoTx(s"==== $identifier wait for busy spat sector ${sector + 1} ====")
          Await.ready(spatFuts(sector), Duration.Inf)
        }
        // differance process
        val (spatFut, stepFut) =
          cursor.atomic(s"$identifier : difference algorithm step") { tx =>
            val _spatFut = spat.rotateAndProject(p)(tx)
            val _stepFut = differance.step(tx)
            (_spatFut, _stepFut)
          }

        spatFuts(sector) = spatFut
        val dur = cursor.atomic(s"$identifier : determining rotation duration") { tx =>
          val olap = overlapMotion.step(tx)
          framesToSeconds(p.length) / olap
        }
        val t1 = System.currentTimeMillis()
        logNoTx(s"==== $identifier wait for algorithm step ====")

        Await.ready(stepFut, Duration.Inf)
        stepFut.value.get match {
          case Success(_p) => p = _p
          case Failure(e) =>
            logNoTx(s"==== $identifier : execption in algorithm execution ====")
            e.printStackTrace()
        }

        val dur2 = (System.currentTimeMillis() - t1) * 0.001
        val dur3 = dur - dur2
        if (dur3 > 0.0) {
          logNoTx(s"==== $identifier waiting $dur3 secs to next rotation ====")
          ???
//          receiveWithin((dur3 * 1000).toLong) {
//            case TIMEOUT =>
//          }
        }
        sector = (sector + 1) % numSectors

      } catch {
        case NonFatal(e) =>
          logNoTx(s"==== $identifier : caught exception ====")
          e.printStackTrace()
          if (WritingMachine.restartUponException) {
            WritingMachine.restart()
          } else {
            Thread.sleep(1000)
          }
      }
    }
  }

  def start(): Unit = act()

  def stop(): Unit =
    warnToDo("Init : stop")
}