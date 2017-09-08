/*
 *  SignalLimiter.scala
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

import impl.{DummyLimiterImpl => Impl} // XXX

object SignalLimiter {
  /**
    * Default ceiling is -0.2 dB.
    */
  def apply(lookAhead: Int, ceil: Float = 0.977f): SignalLimiter = Impl(lookAhead, ceil)
}

trait SignalLimiter {
  def process(in: Array[Float], inOff: Int, out: Array[Float], outOff: Int, len: Int): Int

  def latency: Int
}