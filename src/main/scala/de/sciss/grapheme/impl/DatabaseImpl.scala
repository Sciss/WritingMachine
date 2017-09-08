/*
 *  DatabaseImpl.scala
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

import java.io.{File, FileFilter}

import de.sciss.lucre.stm
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.stm.TxnLike.peer
import de.sciss.span.Span
import de.sciss.synth.io.{AudioFile, AudioFileSpec}

import scala.collection.immutable.{IndexedSeq => Vec}
import scala.concurrent.Future
import scala.util.control.NonFatal

object DatabaseImpl {
  private val normName    = "feat_norms.aif"
  private val dirPrefix   = "database"
//  private val xmlName     = "grapheme.xml"
  private val audioName   = "grapheme.aif"

  private val identifier  = "database-impl"

  def apply[S <: Sys[S]](dir: File)(implicit tx: S#Tx, cursor: stm.Cursor[S]): Database[S] = {
    val normFile = new File(dir, normName)
    require(normFile.isFile, s"Missing normalization file at $normFile")

    val arr = dir.listFiles(new FileFilter {
      def accept(f: File): Boolean = f.isDirectory && f.getName.startsWith(dirPrefix)
    })
    val subs = (if (arr == null) Nil else arr.toList).sortBy(f => -f.lastModified()) // newest first

    // left-overs from aborted apps
    subs.drop(1).foreach(deleteDir)

    val /*(*/ spec /*, extr)*/ = subs.headOption match {
      case Some(sub) =>
        val fAudio = new File(sub, audioName)
        if (fAudio.isFile) {
          try {
            val spec = AudioFile.readSpec(fAudio)
            Some((fAudio, spec))
          } catch {
            case NonFatal(e) =>
              e.printStackTrace()
              None // (None, None)
          }
        } else None // (None, None)

      case None => None // (None, None)
    }

    new DatabaseImpl[S](dir, normFile, State(spec, None /* extr */))
  }

  /*
   * Assumes the directory is empty or contains only ordinary files.
   */
  private def deleteDir(dir: File): Boolean = {
    val arr = dir.listFiles()
    val sub = if (arr == null) Nil else arr.toList
    sub.forall(_.delete()) && dir.delete()
  }

  private def copyAudioFile(source: File, target: File)(implicit tx: Tx): Future[Unit] = {
    import GraphemeUtil._

    threadFuture(s"$identifier : copy file") {
      val afSrc = AudioFile.openRead(source)
      try {
        val afTgt = AudioFile.openWrite(target, afSrc.spec)
        try {
          afSrc.copyTo(afTgt, afSrc.numFrames)
          () // oops... old stuff, copyTo should return Unit these days
        } finally {
          afTgt.close()
        }
      } finally {
        afSrc.close()
      }
    }
  }

  final case class State(spec: Option[(File, AudioFileSpec)], extr: Option[File])

}

class DatabaseImpl[S <: Sys[S]] private(dir: File, normFile: File, state0: DatabaseImpl.State)
                                       (implicit val cursor: stm.Cursor[S])
  extends Database[S] with ExtractionImpl[S] {

  import DatabaseImpl._
  import GraphemeUtil._

  def identifier: String = DatabaseImpl.identifier

  private val stateRef = Ref(state0)

  def append(appFile: File, offset: Long, length: Long)(implicit tx: S#Tx): Future[Unit] = {
    val oldFileO = stateRef().spec.map(_._1)
    threadFuture(s"$identifier : append ${formatSeconds(framesToSeconds(length))}") {
      appendBody(oldFileO, appFile, offset, length)
    }
  }

  def remove(instructions: Vec[RemovalInstruction])(implicit tx: S#Tx): Future[Unit] = {
    val len = length
    val filtered = instructions.filter(i => i.span.nonEmpty && i.span.start >= 0 && i.span.stop <= len)
    val sorted = filtered.sortBy(_.span.start)

    val merged = {
      var pred = RemovalInstruction(Span(-1L, -1L), 0L)
      var res = Vec.empty[RemovalInstruction]
      sorted.foreach { succ =>
        pred.merge(succ) match {
          case Some(m) =>
            pred = m
          case None =>
            res :+= pred
            pred = succ
        }
      }
      if (pred.span.nonEmpty) res :+= pred
      res
    }

    val oldFileO = stateRef().spec.map(_._1)
    oldFileO match {
      case Some(f) =>
        threadFuture(s"$identifier : remove") {
          // add a dummy instruction to the end, so we can easier traverse the list
          removalBody(f, merged :+ RemovalInstruction(Span(len, len), 0L))
        }
      case None => futureOf(())
    }
  }

  private def updateLoop(fun: AudioFile => Unit): Unit = {
    val sub   = createDir(dir, dirPrefix)
    val fNew  = new File(sub, audioName)
    val afNew = openMonoWrite(fNew)
    try {
      fun(afNew)
      afNew.close()
      cursor.atomic(s"$identifier : update finalize") { tx =>
        val oldState = stateRef.swap(State(Some((fNew, afNew.spec)), None))(tx.peer)
        val oldFileO2 = oldState.spec.map(_._1)
        tx.afterCommit {
          oldFileO2.foreach { fOld2 =>
            deleteDir(fOld2.getParentFile)
          }
        }
      }
    } finally {
      if (afNew.isOpen) afNew.close()
    }
  }

  private def removalBody(fOld: File, entries: Vec[RemovalInstruction]): Unit = {
    import DSP._

    updateLoop { afNew =>
      val afOld   = AudioFile.openRead(fOld)
      var shrink  = 0L
      var pos     = 0L
      val lim     = SignalLimiter(secondsToFrames(0.1).toInt)
      val inBuf   = afOld.buffer(8192)
      val fInBuf  = inBuf(0)
      val outBuf  = afOld.buffer(8192)
      val fOutBuf = outBuf(0)
      var written = 0L
      entries.foreach { entry =>
        val start = entry.span.start - shrink
        // copy
        while (pos < start) {
          val chunkLen = min(start - pos, 8192).toInt
          afOld.read(inBuf, 0, chunkLen)
          val chunkLen2 = lim.process(fInBuf, 0, fOutBuf, 0, chunkLen)
          afNew.write(outBuf, 0, chunkLen2)
          written += chunkLen2
          pos += chunkLen
        }
        // fade
        val fdLen = entry.fade
        if (fdLen > 0) {
          var done = 0L
          val fo = SignalFader(0L, fdLen, 1f, 0f, 0.6666f)
          val fi = SignalFader(0L, fdLen, 0f, 1f, 0.6666f)
          while (done < fdLen) {
            val chunkLen = min(fdLen - done, 8192).toInt
            afOld.seek(entry.span.start + done)
            afOld.read(inBuf, 0, chunkLen)
            afOld.seek(entry.span.stop - fdLen + done)
            afOld.read(outBuf, 0, chunkLen)
            fo.process(fInBuf, 0, fInBuf, 0, chunkLen)
            fi.process(fOutBuf, 0, fOutBuf, 0, chunkLen)
            add(fOutBuf, 0, fInBuf, 0, chunkLen)
            val chunkLen2 = lim.process(fInBuf, 0, fOutBuf, 0, chunkLen)
            afNew.write(outBuf, 0, chunkLen2)
            written += chunkLen2
            done += chunkLen
          }
          pos += entry.fade
        }
        shrink += entry.span.length - entry.fade
      }
      clear(fInBuf, 0, 8192)
      while (written < pos) {
        val chunkLen = math.min(pos - written, 8192).toInt
        val chunkLen2 = lim.process(fInBuf, 0, fOutBuf, 0, chunkLen)
        afNew.write(outBuf, 0, chunkLen2)
        written += chunkLen2
      }
    }
  }

  private def appendBody(oldFileO: Option[File], appFile: File, off0: Long, len0: Long): Unit = {
    import DSP._

    updateLoop { afNew =>
      val afApp = AudioFile.openRead(appFile)
      //println( "app file = " + appFile + "; numFrames = " + afApp.numFrames + "; off = " + offset + " ; len = " + len )

      // this is important: DiskOut writes in blocks, and therefore the actual
      // number of frames in afApp may be a bit smaller than the len0 argument!
      val off = min(off0, afApp.numFrames)
      val len = min(len0, afApp.numFrames - off)

      try {
        if (off > 0) afApp.seek(off)
        val fdLen = oldFileO.map(fOld => {
          val afOld = AudioFile.openRead(fOld)
          try {
            require(afOld.numChannels == afApp.numChannels, identifier + " : append - channel mismatch")
            val _fdLen = min(afOld.numFrames, len, secondsToFrames(0.1)).toInt
            afOld.copyTo(afNew, afOld.numFrames - _fdLen)
            if (_fdLen > 0) {
              val fo = SignalFader(0L, _fdLen, 1f, 0f) // make it linear, so we don't need a limiter
              val foBuf = afOld.buffer(_fdLen)
              afOld.read(foBuf)
              fo.process(foBuf(0), 0, foBuf(0), 0, _fdLen)
              val fi = SignalFader(0L, _fdLen, 0f, 1f)
              val fiBuf = afApp.buffer(_fdLen)
              afApp.read(fiBuf)
              fi.process(fiBuf(0), 0, fiBuf(0), 0, _fdLen)
              add(fiBuf(0), 0, foBuf(0), 0, _fdLen)
              afNew.write(fiBuf)
            }
            _fdLen
          } finally {
            afOld.close()
          }
        }).getOrElse(0)
        afApp.copyTo(afNew, len - fdLen)
      } finally {
        afApp.close()
      }
    }
  }

  def length(implicit tx: S#Tx): Long = stateRef().spec.map(_._2.numFrames).getOrElse(0L)

  def reader(implicit tx: S#Tx): FrameReader.Factory = {
    val f = stateRef().spec.map(_._1).getOrElse(sys.error(identifier + " : contains no file"))
    FrameReader.Factory(f)
  }

  def asStrugatziDatabase(implicit tx: S#Tx): Future[File] = {
    val state = stateRef()
    state.extr match {
      case Some(meta) => futureOf(meta.getParentFile)
      case None =>
        val audioInput = state.spec.map(_._1).getOrElse(sys.error(identifier + " : contains no file"))
        val sub = audioInput.getParentFile
        copyAudioFile(normFile, new File(sub, normName)).flatMap { _ =>
          extractAndUpdate(audioInput, sub)
        }
    }
  }

  private def extractAndUpdate(audioInput: File, sub: File): Future[File] = {
    cursor.atomic(s"$identifier : extract") { implicit tx =>
      extract(audioInput, Some(sub), keep = true).map { meta =>
        assert(meta.getParentFile == sub)
        updateState(meta)
        sub
      }
    }
  }

  private def updateState(meta: File): Unit = {
    cursor.atomic(s"$identifier : cache feature extraction") { implicit tx =>
      stateRef.transform(_.copy(extr = Some(meta)))
    }
  }
}