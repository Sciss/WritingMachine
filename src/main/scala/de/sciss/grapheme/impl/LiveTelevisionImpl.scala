/*
 *  LiveTelevisionImpl.scala
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
package impl

import java.io.File

import de.sciss.lucre.stm.Sys
import de.sciss.lucre.stm.TxnLike.peer
import de.sciss.synth.proc.Proc

import scala.concurrent.{Future, Promise}

object LiveTelevisionImpl {
  private val identifier = "live-television-impl"

  def apply[S <: Sys[S]](): Television[S] = new LiveTelevisionImpl[S]()
}

final class LiveTelevisionImpl[S <: Sys[S]] private() extends Television[S] {

  import GraphemeUtil._
  import LiveTelevisionImpl._

  val lookAheadLim = 0.01

  def latency: Double = lookAheadLim * 2

  private val procRef = Ref(Option.empty[Proc[S]])
  private val futRef = Ref({
    val ev = Future.failed[File](
      new RuntimeException(s"$identifier : capture file not yet initialized"))
    ev
  })

  def capture(length: Long)(implicit tx: S#Tx): Future[File] = {
//    import DSL._

    val dur = framesToSeconds(length) + latency
    val res = Promise[File]()
    val oldFut = futRef.swap(res.future)

    ???

//    require(oldFut.isSet, s"$identifier : still in previous capture")
//
//    val p = procRef().getOrElse {
//      val fact = diff("$live-tv") {
//        val pBoost = pControl("boost", ParamSpec(1.0, 10.0, ExpWarp), 1.0)
//        val pDur = pScalar("dur", ParamSpec(0.0, 600.0), 10.0)
//        graph { in0: In =>
//          val path = createTempFile(".aif", None, keep = false)
//          val in = if (WritingMachine.tvPhaseFlip) in0 * Seq(1, -1) else in0
//          val mix = Limiter.ar(Mix.mono(in) * pBoost.kr, 0.97, 0.01)
//          val buf = bufRecord(path.getAbsolutePath, 1, AudioFileType.AIFF, SampleFormat.Int24)
//          val dura = pDur.ir
//          val me = Proc.local
//          Done.kr(Line.kr(dur = dura)).react {
//            thread(identifier + " : capture completed") {
//              atomic(identifier + " stop process") { implicit tx => me.stop }
//              var len = 0L
//              var i = 10
//              // some effort to make sure the file was closed by the server
//              // ; unfortunately we can't guarantee this with the current
//              // sound processes (or can we?)
//              var e: Throwable = null
//              while (len == 0L && i > 0) {
//                try {
//                  val spec = AudioFile.readSpec(path)
//                  len = spec.numFrames
//                } catch {
//                  case NonFatal(_e) => e = _e
//                }
//                if (len == 0L) Thread.sleep(200)
//                i -= 1
//              }
//              atomic(identifier + " return path") { implicit tx =>
//                futRef().set(if ((len == 0L) && (e != null)) {
//                  Future.Failure(e)
//                } else {
//                  Future.Success(path)
//                })
//              }
//            }
//          }
//          DiskOut.ar(buf.id, mix)
//        }
//      }
//      val _p = fact.make
//      procRef.set(Some(_p))
//      _p.audioInput("in").bus = Some(RichBus.soundIn(Server.default,
//        WritingMachine.tvNumChannels, WritingMachine.tvChannelOffset))
//      _p.control("boost").v = WritingMachine.tvBoostDB.dbamp
//      _p
//    }
//
//    p.control("dur").v = dur
//    p.play

    // XXX TODO : this should be somewhat handled (ProcTxn needs addition)
    //      tx.afterFailure { e => res.fail( e )}

    res.future
  }
}