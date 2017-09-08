/*
 *  SignalFader.scala
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

import de.sciss.grapheme.impl.{SignalFaderImpl => Impl}

object SignalFader {
  def apply(off: Long, len: Long, start: Float, stop: Float, pow: Float = 1f): SignalFader =
    Impl(off, len, start, stop, pow)
}

trait SignalFader {
  def process(in: Array[Float], inOff: Int, out: Array[Float], outOff: Int, len: Int): Unit
}