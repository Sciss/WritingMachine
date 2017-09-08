/*
 *  DummyLimiterImpl.scala
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

object DummyLimiterImpl extends SignalLimiter {
  def apply(lookAhead: Int, ceil: Float = 0.977f): SignalLimiter = this

  def latency = 0

  def process(in: Array[Float], inOff: Int, out: Array[Float], outOff: Int, len: Int): Int = {
    System.arraycopy(in, inOff, out, outOff, len)
    len
  }
}