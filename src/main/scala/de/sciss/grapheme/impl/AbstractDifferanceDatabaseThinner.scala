/*
 *  AbstractDifferanceDatabaseThinner.scala
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

import de.sciss.lucre.stm.Sys
import de.sciss.span.Span

import collection.immutable.{IndexedSeq => Vec}
import scala.concurrent.Future

abstract class AbstractDifferanceDatabaseThinner[S <: Sys[S]] extends DifferanceDatabaseThinner[S] {

  import GraphemeUtil._

  /**
    * Cross-fade punch-in duration in seconds
    */
  def fadeInMotion: Motion

  /**
    * Cross-fade punch-out duration in seconds
    */
  def fadeOutMotion: Motion

  /**
    * Factor at which the removed spans shrink (0 = no shrinking, 1 = removal not carried out)
    */
  def shrinkMotion: Motion

  /**
    * Position jitter in seconds
    */
  def jitterMotion: Motion

  def database: Database[S]

  def limiter(): SignalLimiter

  def inFader (off: Long, len: Long): SignalFader
  def outFader(off: Long, len: Long): SignalFader

  def remove(spans: Vec[Span])(implicit tx: S#Tx): Future[Unit] = {
    val dbLen = database.length
    val instrs = spans.map { span =>
      val shrink    = shrinkMotion.step
      val jitter    = secondsToFrames(jitterMotion.step)
      val start0    = (span.start + jitter + shrink * (span.length / 2)).toLong
      val stop0     = (span.stop + jitter - shrink * ((span.length + 1) / 2)).toLong
      val start     = max(0L, min(dbLen, start0))
      val stop      = max(start, min(dbLen, stop0))
      val spanT     = Span(start, stop)

      val fade0     = secondsToFrames(fadeInMotion.step)
      val fiPre     = min(spanT.start, fade0 / 2)
      val foPost    = min(dbLen - spanT.stop, fade0 / 2)

      val fiPost0   = fade0 - fiPre
      val foPre0    = fade0 - foPost
      val innerSum  = fiPost0 + foPre0

      val (fiPost, foPre) = if (innerSum <= spanT.length) {
        (fiPost0, foPre0)
      } else {
        val scl = spanT.length.toDouble / innerSum
        ((fiPost0 * scl).toLong, (foPre0 * scl).toLong)
      }

      val spanTFd = Span(spanT.start - fiPre, spanT.stop + foPost)
      val fade = min(fiPre + fiPost, foPre + foPost)

      RemovalInstruction(spanTFd, fade)
    }

    database.remove(instrs)
  }
}