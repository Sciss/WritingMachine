/*
 *  FrameReaderFactoryImpl.scala
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

import java.io.File

import de.sciss.synth.io.{AudioFile, Frames}

object FrameReaderFactoryImpl {
  def apply(file: File): FrameReader.Factory = new FrameReader.Factory {
    def open(): FrameReader = {
      val af = AudioFile.openRead(file)
      new ReaderImpl(af)
    }
  }

  private final class ReaderImpl(af: AudioFile) extends FrameReader {
    override def toString = s"FrameReader(${af.file.getOrElse("?")})"

    def read(buf: Frames, off: Long, len: Int): Unit = {
      if (off != af.position) {
        af.position = off
      }
      af.read(buf, 0, len)
    }

    def close(): Unit =
      af.close()
  }
}