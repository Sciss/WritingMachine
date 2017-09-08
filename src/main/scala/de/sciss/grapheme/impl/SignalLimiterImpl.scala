/*
 *  SignalLimiterImpl.scala
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

/**
  * A more or less literal translation of SuperCollider's `Limiter` UGen.
  */
object SignalLimiterImpl {
  def apply(lookAhead: Int, ceil: Float = 0.977f): SignalLimiter = new SignalLimiterImpl(lookAhead, ceil)
}

final class SignalLimiterImpl private(lookAhead: Int, ceil: Float) extends SignalLimiter {
  private[this] var flips       = 0
  private[this] var pos         = 0
  private[this] var slope       = 0.0f
  private[this] var level       = 1.0f
  private[this] var prevMaxVal  = 0.0f
  private[this] var curMaxVal   = 0.0f
  private[this] val slopeFactor = 1.0f / lookAhead

  private[this] val xBuf        = new Array[Float](lookAhead * 3 /* 4 */)
  private[this] var xInOff      = 0
  private[this] var xMidOff     = lookAhead
  private[this] var xOutOff     = lookAhead * 2

  def process(in: Array[Float], inOff: Int, out: Array[Float], outOff: Int, len: Int): Int = {
    var remain    = len
    var bufRemain = lookAhead - pos
    var inPos     = inOff
    var outPos    = outOff

    while (remain > 0) {
      val nSmps   = math.min(remain, bufRemain)
      var xInPos  = xInOff + pos
      var xOutPos = xOutOff + pos
      val stop    = inPos + nSmps
      if (flips >= 2) {
        //assert( latency == 0 )
        while (inPos < stop) {
          val v = in(inPos)
          inPos   += 1
          xBuf(xInPos) = v
          xInPos  += 1
          out(outPos) = level * xBuf(xOutPos)
          outPos  += 1
          xOutPos += 1
          level += slope
          val va = math.abs(v)
          if (va > curMaxVal) curMaxVal = va
        }
      } else {
        while (inPos < stop) {
          val v = in(inPos)
          inPos += 1
          xBuf(xInPos) = v
          xInPos += 1
          level += slope
          val va = math.abs(v)
          if (va > curMaxVal) curMaxVal = va
        }
      }

      pos += nSmps

      if (pos >= lookAhead) {
        pos = 0
        bufRemain = lookAhead

        val maXVal2 = math.max(prevMaxVal, curMaxVal)
        prevMaxVal = curMaxVal
        curMaxVal = 0.0f

        val nextLevel = if (maXVal2 > ceil) {
          ceil / maXVal2
        } else {
          1.0f
        }

        slope = (nextLevel - level) * slopeFactor

        val temp  = xOutOff
        xOutOff   = xMidOff
        xMidOff   = xInOff
        xInOff    = temp

        flips += 1
      }
      remain -= nSmps
    }

    outPos - outOff
  }

  def latency: Int = lookAhead * 2
}