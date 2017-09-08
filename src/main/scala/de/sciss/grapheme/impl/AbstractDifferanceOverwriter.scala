/*
 *  AbstractDifferanceOverwriter.scala
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

import de.sciss.grapheme.DifferanceDatabaseQuery.Match
import de.sciss.lucre.stm
import de.sciss.lucre.stm.Sys
import de.sciss.span.Span

import scala.concurrent.Future

object AbstractDifferanceOverwriter {
  var verbose = false
}

abstract class AbstractDifferanceOverwriter[S <: Sys[S]] extends DifferanceOverwriter[S] {

  import GraphemeUtil._
  import AbstractDifferanceOverwriter._

  protected implicit def cursor: stm.Cursor[S]

  private[this] val identifier = "a-overwriter"

  /**
    * Cross-fade punch-in duration in seconds
    */
  def fadeInMotion: Motion

  /**
    * Cross-fade punch-out duration in seconds
    */
  def fadeOutMotion: Motion

  def limiter(): SignalLimiter

  def inFader(off: Long, len: Long): SignalFader

  def outFader(off: Long, len: Long): SignalFader

  def ramp(off: Long, len: Long, start: Float, stop: Float): SignalFader

  def perform(phrase: Phrase[S], source: OverwriteInstruction, target: Match[S])
             (implicit tx: S#Tx): Future[Phrase[S]] = {

    val pLen    = phrase.length
    val pSpan0  = source.span.intersect(Span(0L, pLen))
    pSpan0 match {
      case Span.Void => futureOf(phrase)
      case pSpan1: Span =>
        val db        = target.database
        val dbLen     = db.length
        val dbSpan0   = target.span.intersect(Span(0L, dbLen))
        dbSpan0 match {
          case Span.Void => futureOf(phrase)
          case dbSpan1: Span =>
            perform1(phrase = phrase, source = source, target = target, pLen = pLen, pSpan = pSpan1, dbLen = dbLen, dbSpan = dbSpan1)
        }
    }
  }

  private def perform1(phrase: Phrase[S], source: OverwriteInstruction, target: Match[S],
                       pLen: Long, pSpan: Span,
                       dbLen: Long, dbSpan: Span)
             (implicit tx: S#Tx): Future[Phrase[S]] = {

    val db        = target.database
    val fadeIn0   = secondsToFrames(fadeInMotion.step)
    val fadeOut0  = secondsToFrames(fadeOutMotion.step)
    val fiPre     = min(pSpan.start, dbSpan.start, fadeIn0 / 2)
    val foPost    = min(pLen - pSpan.stop, dbLen - dbSpan.stop, fadeOut0 / 2)

    val fiPost0 = fadeIn0 - fiPre
    val foPre0 = fadeOut0 - foPost
    val innerSum = fiPost0 + foPre0

    val (fiPost, foPre) = if (innerSum <= pSpan.length && innerSum <= dbSpan.length) {
      (fiPost0, foPre0)
    } else {
      val scl = min(pSpan.length, dbSpan.length).toDouble / innerSum
      ((fiPost0 * scl).toLong, (foPre0 * scl).toLong)
    }

    val pSpanFd   = Span(pSpan.start - fiPre, pSpan.stop + foPost)
    val dbSpanFd  = Span(dbSpan.start - fiPre, dbSpan.stop + foPost)
    val fadeIn    = fiPre + fiPost
    val fadeOut   = foPre + foPost

    val pReaderF  = phrase.reader
    val dbReaderF = db.reader

    if (verbose) {
      println(s"\n-------- overwriter : src ${source.span}, ovr ${target.span}")
      println(s"-------- fadein0 $fadeIn0, fadeout0 $fadeOut0, p-len $pLen, db-len $dbLen")
      println(s"-------- thread body: p-span-fd $pSpanFd, db-span-fd $dbSpanFd, fadein $fadeIn, fadeout $fadeOut")
    }

    threadFuture(s"$identifier : perform ") {
      threadBody(pReaderF, dbReaderF, pSpanFd, dbSpanFd, target.boostIn, target.boostOut, fadeIn, fadeOut, pLen, dbLen)
    }
  }

  private def threadBody(sourceReaderF: FrameReader.Factory, overReaderF: FrameReader.Factory,
                         sourceSpan: Span, overSpan: Span, boostIn: Float, boostOut: Float,
                         fadeIn: Long, fadeOut: Long, phraseLen: Long, overLen: Long): Phrase[S] = {

    import DSP._

    val afSrc = sourceReaderF.open()
    try {
      val afOvr = overReaderF.open()
      try {
        val fTgt = createTempFile(".aif", None, keep = false)
        val afTgt = openMonoWrite(fTgt)
        try {
          val bufSrc = afTgt.buffer(8192)
          val fBufSrc = bufSrc(0)
          val bufOvr = afTgt.buffer(8192)
          val fBufOvr = bufOvr(0)
          val bufTgt = afTgt.buffer(8192)
          val fBufTgt = bufTgt(0)

          def readOvr(off: Long, len: Int): Unit = {
            val len2 = math.max(0, math.min(overLen - off, len)).toInt
            afOvr.read(bufOvr, off, len2)
            if (len2 < len) {
              clear(fBufOvr, len2, len - len2)
            }
          }

          // pre
          var off = 0L
          var stop = sourceSpan.start
          if (verbose) println("-------- COPY PRE FROM " + afSrc + ". SPAN = " + Span(off, stop))
          while (off < stop) {
            val chunkLen = math.min(stop - off, 8192).toInt
            afSrc.read(bufSrc, off, chunkLen)
            afTgt.write(bufSrc, 0, chunkLen)
            off += chunkLen
          }

          // fadein
          val lim = limiter()
          val fSrcOut = outFader(0L, fadeIn)
          val fOvrIn = inFader(0L, fadeIn)
          val rampOvr = ramp(0L, overSpan.length, boostIn, boostOut)
          stop += fadeIn
          var ovrOff = overSpan.start
          //               var ovrOff2 = overSpan.start

          if (verbose) println("-------- FADEIN FROM " + afOvr + ". SPAN = " + Span(ovrOff, ovrOff + (stop - off)))
          while (off < stop) {
            val chunkLen = math.min(stop - off, 8192).toInt
            readOvr(ovrOff, chunkLen)
            rampOvr.process(fBufOvr, 0, fBufSrc, 0, chunkLen)
            fOvrIn.process(fBufOvr, 0, fBufOvr, 0, chunkLen)
            val chunkLen2 = lim.process(fBufOvr, 0, fBufTgt, 0, chunkLen)
            afSrc.read(bufSrc, off, chunkLen2)
            fSrcOut.process(fBufSrc, 0, fBufSrc, 0, chunkLen2)
            add(fBufSrc, 0, fBufTgt, 0, chunkLen2)
            afTgt.write(bufTgt, 0, chunkLen2)
            ovrOff += chunkLen
            off += chunkLen2
            //                  ovrOff2  += chunkLen2
          }

          // over
          // off = sourceSpan.start + fadeIn
          off = 0
          stop = overSpan.length - (fadeIn + fadeOut)
          if (verbose) println("-------- OVERWRITING. SPAN = " + Span(ovrOff, ovrOff + (stop - off)))
          while (off < stop) {
            val chunkLen = math.min(stop - off, 8192).toInt
            readOvr(ovrOff, chunkLen)
            rampOvr.process(fBufOvr, 0, fBufSrc, 0, chunkLen)
            val chunkLen2 = lim.process(fBufOvr, 0, fBufTgt, 0, chunkLen)
            afTgt.write(bufTgt, 0, chunkLen2)
            ovrOff += chunkLen
            off += chunkLen2
          }

          // fadeout
          val fSrcIn = inFader(0L, fadeOut)
          val fOvrOut = outFader(0L, fadeOut)
          off = sourceSpan.stop - fadeOut
          stop = off + fadeOut
          if (verbose) println("-------- FADEOUT. SPAN (SRC) = " + Span(off, stop))
          while (off < stop) {
            val chunkLen = math.min(stop - off, 8192).toInt
            readOvr(ovrOff, chunkLen)
            rampOvr.process(fBufOvr, 0, fBufSrc, 0, chunkLen)
            fOvrOut.process(fBufOvr, 0, fBufOvr, 0, chunkLen)
            val chunkLen2 = lim.process(fBufOvr, 0, fBufTgt, 0, chunkLen)
            afSrc.read(bufSrc, off, chunkLen2)
            fSrcIn.process(fBufSrc, 0, fBufSrc, 0, chunkLen2)
            add(fBufSrc, 0, fBufTgt, 0, chunkLen2)
            afTgt.write(bufTgt, 0, chunkLen2)
            ovrOff += chunkLen
            off += chunkLen2
          }

          // post
          stop = phraseLen
          if (verbose) println("-------- COPY POST TO " + fTgt + ". SPAN (SRC) = " + Span(off, stop))
          while (off < stop) {
            val chunkLen = math.min(stop - off, 8192).toInt
            afSrc.read(bufSrc, off, chunkLen)
            afTgt.write(bufSrc, 0, chunkLen)
            off += chunkLen
          }
          //               Phrase.fromFile( fTgt )
          //               fTgt
          afTgt.close()
          cursor.atomic(s"$identifier : phrase from file") { implicit tx =>
            Phrase.fromFile[S](fTgt)
          }

        } finally {
          if (afTgt.isOpen) afTgt.close()
        }
      } finally {
        afOvr.close()
      }
    } finally {
      afSrc.close()
    }
  }
}