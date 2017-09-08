/*
 *  GraphemeUtil.scala
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

import java.io.File

import de.sciss.lucre.stm
import de.sciss.lucre.stm.Sys
import de.sciss.span.Span
import de.sciss.synth.io.{AudioFile, AudioFileSpec, AudioFileType, SampleFormat}

import scala.concurrent.{Future, Promise}
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

object GraphemeUtil {
  var logTransactions = true
  /**
    * Important: there seem to be problems with communications between
    * WritingMachine and scsynth when using `sys.props( "java.io.tmpdir" )`
    * which gives you something terrible as `/var/folders/M9/M9W+ucwpFzaqGpC2mu3KZq0sUCc/-Tmp-/`
    */
  var tmpDir = new File("/tmp")

  val seed: Long = System.currentTimeMillis() // 0L
  private val rng = new util.Random(seed)

  val deleteTempFilesOnExit = true // false

  def logNoTx(text: => String): Unit =
    if (logTransactions) logPrint(text)

  def logTx(text: => String)(implicit tx: Tx): Unit =
    if (logTransactions) tx.afterCommit(logPrint(text))

  private def logPrint(text: String): Unit =
    println(s"${timeString()} $text")

  def fileNameWithoutExtension(f: File): String = {
    val n = f.getName
    val i = n.lastIndexOf('.')
    if (i < 0) n else n.substring(0, i)
  }

  def formatSpan(span: Span): String = {
    val sb = new StringBuilder(24)
    sb.append('[')
    sb.append(formatSeconds(framesToSeconds(span.start)))
    sb.append('-')
    sb.append(formatSeconds(framesToSeconds(span.stop)))
    sb.append(']')
    sb.toString()
  }

  def formatPercent(d: Double): String = {
    val pm = (d * 1000).toInt
    val post = pm % 10
    val pre = pm / 10
    val sb = new StringBuilder(8)
    sb.append(pre)
    sb.append('.')
    sb.append(post)
    sb.append('%')
    sb.toString()
  }

  def timeString(): String = new java.util.Date().toString

  def formatSeconds(seconds: Double): String = {
    val millisR = (seconds * 1000).toInt
    val sb      = new StringBuilder(10)
    val secsR   = millisR / 1000
    val millis  = millisR % 1000
    val mins    = secsR / 60
    val secs    = secsR % 60
    if (mins > 0) {
      sb.append(mins)
      sb.append(':')
      if (secs < 10) {
        sb.append('0')
      }
    }
    sb.append(secs)
    sb.append('.')
    if (millis < 10) {
      sb.append('0')
    }
    if (millis < 100) {
      sb.append('0')
    }
    sb.append(millis)
    sb.append('s')
    sb.toString()
  }

  def random /* (implicit tx: Tx) */: Double = rng.nextDouble()

  def sampleRate: Double = 44100.0

  def random(top: Int) /* (implicit tx: Tx) */: Int = (random * top).toInt

  def secondsToFrames(secs: Double): Long = (secs * sampleRate + 0.5).toLong

  def framesToSeconds(frames: Long): Double = frames / sampleRate

  def max(i: Int    , is: Int*    ): Int    = is.foldLeft(i)(_ max _)
  def max(n: Long   , ns: Long*   ): Long   = ns.foldLeft(n)(_ max _)
  def max(d: Double , ds: Double* ): Double = ds.foldLeft(d)(_ max _)

  def min(i: Int    , is: Int*    ): Int    = is.foldLeft(i)(_ min _)
  def min(n: Long   , ns: Long*   ): Long   = ns.foldLeft(n)(_ min _)
  def min(d: Double , ds: Double* ): Double = ds.foldLeft(d)(_ min _)

  def openMonoWrite(f: File): AudioFile =
    AudioFile.openWrite(f, AudioFileSpec(AudioFileType.AIFF, SampleFormat.Float, 1, sampleRate))

  def databaseDir: File = WritingMachine.databaseDir

  def createTempFile(suffix: String, dir: Option[File], keep: Boolean): File = {
    val res = File.createTempFile("grapheme", suffix, dir.getOrElse(tmpDir))
    if (!keep && deleteTempFilesOnExit) res.deleteOnExit()
    res
  }

  def createDir(parent: File, prefix: String): File = {
    val f = File.createTempFile(prefix, "", parent)
    f.delete()
    require(f.mkdir(), s"Could not create directory : $f")
    f
  }

  def futureOf[A](value: A): Future[A] = Future.successful(value)

  def threadFuture[A](name: String)(code: => A)(implicit tx: Tx): Future[A] = {
    val ev = Promise[A]()
    tx.afterCommit {
      new Thread(name) {
        start()

        override def run(): Unit = {
          logNoTx(s"threadFuture started : $name")
          ev.complete(try {
            val res = code
            Success(res)
          } catch {
            case NonFatal(e) => Failure(e)
          })
        }
      }
    }
    ev.future
  }

  def warnToDo(what: => String): Unit =
    logNoTx(s"+++MISSING+++ $what")

  def thread(name: String)(code: => Unit): Unit = {
    new Thread(name) {
      start()

      override def run(): Unit = code
    }
  }

  def threadAtomic[S <: Sys[S]](info: => String)(fun: S#Tx => Unit)(implicit cursor: stm.Cursor[S]): Unit = {
    thread(info) {
      cursor.atomic(info)(fun(_))
    }
  }
}