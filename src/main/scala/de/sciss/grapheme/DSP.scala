/*
 *  DSP.scala
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

object DSP {
  def add(in: Array[Float], inOff: Int, out: Array[Float], outOff: Int, len: Int): Unit = {
    var i = 0
    while (i < len) {
      out(outOff + i) += in(inOff + i)
      i += 1
    }
  }

  def clear(in: Array[Float], inOff: Int, len: Int): Unit = {
    var i = 0
    while (i < len) {
      in(inOff + i) = 0f
      i += 1
    }
  }
}