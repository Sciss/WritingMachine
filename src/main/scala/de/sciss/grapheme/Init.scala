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
import de.sciss.lucre.synth.{Sys => SSys}
import de.sciss.nuages.NuagesView
import de.sciss.synth.proc.Proc

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

object Init {
  import WritingMachine._

  private val identifier = "meta-diff"

  def apply[S <: SSys[S]](r: NuagesView[S])(implicit tx: S#Tx): Init[S] = {
    val coll = Proc[S] // panel.collector.getOrElse(sys.error("Requires nuages to use collector"))
    val spat = DifferanceSpat[S](coll)
    val tv = if (tvUseTestFile) {
      Television.fromFile[S](new File(testDir, "euronews.aif"))
    } else {
      Television.live[S]()
    }
    import r.cursor
    val i = new Init[S](/* start, diff, */ spat, tv)
    i
  }
}

final class Init[S <: Sys[S]] private(/* _phrase0: Phrase, val differance: DifferanceAlgorithm, */ val spat: DifferanceSpat[S],
                         val tv: Television[S])(implicit cursor: stm.Cursor[S]) {

  import GraphemeUtil._
  import Init._

  val numSectors: Int = WritingMachine.masterNumChannels

  var overlapMotion: Motion = Motion.exprand(1.1, numSectors - 1)

  var keepGoing = true

  private def act(): Unit = {
    val t = new Thread {
      override def run(): Unit = {
        val futP0 = cursor.atomic("meta-diff initial tv capture") { implicit tx =>
          tv.capture(secondsToFrames(WritingMachine.initialPhraseFill))
        }
        logNoTx(s"==== $identifier wait for initial tv capture ====")
        futP0.onComplete {
          case Failure(e) =>
            logNoTx(s"==== $identifier initial capture failed: ====")
            e.printStackTrace()
            Thread.sleep(1000)
          case Success(fP0) => actWithFile(fP0)
        }
      }
    }
    t.start()
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
    val spatFutures = Array.fill(numSectors)(futureOf(()))
    var sector = 0
    while (keepGoing) {
      try {
        if (!spatFutures(sector).isCompleted) {
          logNoTx(s"==== $identifier wait for busy spat sector ${sector + 1} ====")
          Await.ready(spatFutures(sector), Duration.Inf)
        }
        // differance process
        val (spatFut, stepFut) =
          cursor.atomic(s"$identifier : difference algorithm step") { tx =>
            val _spatFut = spat.rotateAndProject(p)(tx)
            val _stepFut = differance.step(tx)
            (_spatFut, _stepFut)
          }

        spatFutures(sector) = spatFut
        val dur = cursor.atomic(s"$identifier : determining rotation duration") { tx =>
          val oLap = overlapMotion.step(tx)
          framesToSeconds(p.length) / oLap
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