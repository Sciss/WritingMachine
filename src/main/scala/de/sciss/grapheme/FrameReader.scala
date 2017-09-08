/*
 *  FrameReader.scala
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

import de.sciss.synth.io.Frames
import impl.{FrameReaderFactoryImpl => Impl}
import java.io.File

object FrameReader {

  object Factory {
    def apply(file: File): Factory = Impl(file)
  }

  trait Factory {
    def open(): FrameReader
  }

}

trait FrameReader {
  def read(buf: Frames, off: Long, len: Int): Unit

  def close(): Unit
}