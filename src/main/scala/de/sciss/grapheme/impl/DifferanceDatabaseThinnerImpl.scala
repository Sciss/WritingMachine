/*
 *  DifferanceDatabaseThinner.scala
 *  (WritingMachine)
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

object DifferanceDatabaseThinnerImpl {
  def apply[S <: Sys[S]](database: Database[S]): DifferanceDatabaseThinner[S] =
    new DifferanceDatabaseThinnerImpl[S](database)
}

final class DifferanceDatabaseThinnerImpl[S <: Sys[S]] private(val database: Database[S])
  extends AbstractDifferanceDatabaseThinner[S] {

  import GraphemeUtil._

  val fadeInMotion: Motion = Motion.exprand(0.02, 2.0)
  val fadeOutMotion: Motion = Motion.exprand(0.03, 3.0)
  val shrinkMotion: Motion = Motion.linrand(0.0, 1.0)
  val jitterMotion: Motion = Motion.linexp(Motion.walk(0.0, 1.0, 0.1), 0.0, 1.0, 0.02222222, 5.0)

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