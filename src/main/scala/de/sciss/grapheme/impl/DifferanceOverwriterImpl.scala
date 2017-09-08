/*
 *  DifferanceOverwriterImpl.scala
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

import de.sciss.lucre.stm
import de.sciss.lucre.stm.Sys

object DifferanceOverwriterImpl {
  def apply[S <: Sys[S]]()(implicit cursor: stm.Cursor[S]): DifferanceOverwriter[S] =
    new DifferanceOverwriterImpl[S]()
}

final class DifferanceOverwriterImpl[S <: Sys[S]] private()(implicit val cursor: stm.Cursor[S])
  extends AbstractDifferanceOverwriter[S] {

  import GraphemeUtil._

  val fadeInMotion: Motion = Motion.exprand(0.02, 2.0)
  val fadeOutMotion: Motion = Motion.exprand(0.03, 3.0)

  def ramp(off: Long, len: Long, start: Float, stop: Float): SignalFader =
    SignalFader(off, len, start, stop)

  /**
    * Uses 1/3 pow which gives some -4 dB cross fade point, a bit weaker than equal power fade
    */
  def inFader(off: Long, len: Long): SignalFader = SignalFader(off, len, 0f, 1f, 0.66666f)

  /**
    * Uses 1/3 pow which gives some -4 dB cross fade point, a bit weaker than equal power fade
    */
  def outFader(off: Long, len: Long): SignalFader = SignalFader(off, len, 1f, 0f, 0.66666f)

  def limiter(): SignalLimiter = SignalLimiter(secondsToFrames(0.100).toInt)
}