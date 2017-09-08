/*
 *  FileTelevisionImpl.scala
 *  (WritingMachine)
 *
 *  Copyright (c) 2011-2017 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either
 *  version 2, june 1991 of the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License (gpl.txt) along with this software; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.grapheme
package impl

import java.io.File

import de.sciss.lucre.stm.Sys
import de.sciss.lucre.stm.TxnLike.peer
import de.sciss.synth.io.{AudioFile, AudioFileSpec}

import scala.concurrent.Future

object FileTelevisionImpl {
  private val identifier = "file-television-impl"

  def apply[S <: Sys[S]](f: File): Television[S] = {
    val spec = AudioFile.readSpec(f)
    require(spec.numChannels == 1, s"$identifier : must be mono" )
    require(spec.numFrames > 0   , s"$identifier : file is empty")
    new FileTelevisionImpl[S](f, spec)
  }
}

class FileTelevisionImpl[S <: Sys[S]] private(f: File, spec: AudioFileSpec) extends Television[S] {

  import FileTelevisionImpl._
  import GraphemeUtil._

  private val posRef = Ref((math.random * (spec.numFrames - 1)).toLong)

  def latency = 0.0

  def capture(length: Long)(implicit tx: S#Tx): Future[File] = {
    val oldPos = posRef()
    threadFuture(s"$identifier : capture") {
      try {
        val fNew = createTempFile(".aif", None, keep = false)
        val afNew = openMonoWrite(fNew)
        try {
          val afTV = AudioFile.openRead(f)
          try {
            var pos = oldPos
            var left = length
            afTV.seek(oldPos)
            while (left > 0L) {
              val chunkLen = min(spec.numFrames - pos, left)
              afTV.copyTo(afNew, chunkLen)
              pos += chunkLen
              if (pos == spec.numFrames) {
                afTV.seek(0L)
                pos = 0L
              }
              left -= chunkLen
            }
            fNew
          } finally {
            afTV.close()
          }
          //            } catch {
          //               case e =>
          //                  println( "FileTelevisionImpl capture - Oops. exception. Should handle" )
          //                  e.printStackTrace()
          //                  fNew // que puede... XXX
        } finally {
          afNew.close()
        }
      }
    }
  }
}