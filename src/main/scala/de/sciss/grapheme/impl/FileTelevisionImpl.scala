/*
 *  FileTelevisionImpl.scala
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