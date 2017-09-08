/*
 *  SignalFaderImpl.scala
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

object SignalFaderImpl {
  def apply(off: Long, len: Long, start: Float, stop: Float, pow: Float = 1f): SignalFader = {
    val lvl = if (pow == 1f) new LinLevel(start, stop) else new PowLevel(start, stop, pow)
    new SignalFaderImpl(off, len, lvl)
  }

  sealed trait Level {
    def apply(w: Float): Float
  }

  private final class LinLevel(start: Float, stop: Float) extends Level {
    def apply(w: Float): Float = (1 - w) * start + w * stop
  }

  private final class PowLevel(start: Float, stop: Float, pow: Float) extends Level {
    def apply(w: Float): Float = math.pow(1 - w, pow).toFloat * start + math.pow(w, pow).toFloat * stop
  }

}

final class SignalFaderImpl private(off: Long, len: Long, lvl: SignalFaderImpl.Level)
  extends SignalFader {
  private var pr = -off

  def process(in: Array[Float], inOff: Int, out: Array[Float], outOff: Int, len: Int): Unit = {
    var i = 0
    val _pr = pr
    while (i < len) {
      val w = math.max(0.0, math.min(1.0, (_pr + i).toDouble / len)).toFloat
      out(outOff + i) = lvl(w) * in(inOff + i)
      i += 1
    }
    pr += len
  }
}