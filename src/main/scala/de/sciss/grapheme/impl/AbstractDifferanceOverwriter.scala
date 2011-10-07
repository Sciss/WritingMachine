/*
 *  AbstractDifferanceOverwriter.scala
 *  (WritingMachine)
 *
 *  Copyright (c) 2011 Hanns Holger Rutz. All rights reserved.
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

object AbstractDifferanceOverwriter {
   var verbose = true
}
abstract class AbstractDifferanceOverwriter extends DifferanceOverwriter {
   import GraphemeUtil._
   import AbstractDifferanceOverwriter._

   /**
    * Cross-fade punch-in duration in seconds
    */
   def fadeInMotion : Motion
   /**
    * Cross-fade punch-out duration in seconds
    */
   def fadeOutMotion : Motion

   def limiter() : SignalLimiter
   def inFader( off: Long, len: Long ) : SignalFader
   def outFader( off: Long, len: Long ) : SignalFader
   def ramp( off: Long, len: Long, start: Float, stop: Float ) : SignalFader

   def perform( phrase: Phrase, source: OverwriteInstruction, target: DifferanceDatabaseQuery.Match )
              ( implicit tx: Tx ) : FutureResult[ Phrase ] = {

      val pLen       = phrase.length
      val pSpan      = source.span.intersect( Span( 0L, pLen ))
      if( pSpan.isEmpty ) return futureOf( phrase )

      val db         = target.database
      val dbLen      = db.length
      val dbSpan     = target.span.intersect( Span( 0L, dbLen ))

//      source.newLength   // ignore, since we have target.span based on this!

      val fadeIn0    = secondsToFrames( fadeInMotion.step )
      val fadeOut0   = secondsToFrames( fadeOutMotion.step )
      val fiPre      = min( pSpan.start, dbSpan.start, fadeIn0/2 )
      val foPost     = min( pLen - pSpan.stop, dbLen - dbSpan.stop, fadeOut0/2 )

//      val fiStop1    = pSpan.start - fiPre + fadeIn0
//      val fiStop2    = dbSpan.start - fiPre + fadeIn0
//      val foStart1   = pStop.stop + foPost - fadeOut0
//      val foStart2   = dbSpan.stop + foPost - fadeOut0

      val fiPost0    = fadeIn0 - fiPre
      val foPre0     = fadeOut0 - foPost
      val innerSum   = fiPost0 + foPre0

      val (fiPost, foPre) = if( innerSum <= pSpan.length && innerSum <= dbSpan.length ) {
         (fiPost0, foPre0)
      } else {
         val scl     = min( pSpan.length, dbSpan.length ).toDouble / innerSum
         ((fiPost0 * scl).toLong, (foPre0 * scl).toLong)
      }

      val pSpanFd    = Span( pSpan.start - fiPre, pSpan.stop + foPost )
      val dbSpanFd   = Span( dbSpan.start - fiPre, dbSpan.stop + foPost )
      val fadeIn     = fiPre + fiPost
      val fadeOut    = foPre + foPost

      val pReaderF   = phrase.reader
      val dbReaderF  = db.reader

      threadFuture( "AbstractDifferanceOverwriter perform" ) {
         threadBody( pReaderF, dbReaderF, pSpanFd, dbSpanFd, target.boostIn, target.boostOut, fadeIn, fadeOut, pLen, dbLen )
      }
   }

   private def add( in: Array[ Float ], inOff: Int, out: Array[ Float ], outOff: Int, len: Int ) {
      var i = 0
      while( i < len ) {
         out( outOff + i ) += in( inOff + i )
         i += 1
      }
   }

   private def clear( in: Array[ Float ], inOff: Int, len: Int ) {
      var i = 0
      while( i < len ) {
         in( inOff + i ) = 0f
         i += 1
      }
   }

   private def threadBody( sourceReaderF: FrameReader.Factory, overReaderF: FrameReader.Factory,
                            sourceSpan: Span, overSpan: Span, boostIn: Float, boostOut: Float,
                            fadeIn: Long, fadeOut: Long, phraseLen: Long, overLen: Long ) : Phrase = {

      val afSrc = sourceReaderF.open()
      try {
         val afOvr = overReaderF.open()
         try {
            val fTgt    = createTempFile( ".aif", None )
            val afTgt   = openMonoWrite( fTgt )
            try {
               val bufSrc  = afTgt.buffer( 8192 )
               val fBufSrc = bufSrc( 0 )
               val bufOvr  = afTgt.buffer( 8192 )
               val fBufOvr = bufOvr( 0 )
               val bufTgt  = afTgt.buffer( 8192 )
               val fBufTgt = bufTgt( 0 )

               def readOvr( off: Long, len: Int ) {
                  val len2 = math.max( 0, math.min( overLen - off, len )).toInt
                  afOvr.read( bufOvr, off, len2 )
                  if( len2 < len ) {
                     clear( fBufOvr, len2, len - len2 )
                  }
               }

               // pre
               var off     = sourceSpan.start
               var stop    = sourceSpan.stop
if( verbose ) println( "COPY PRE FROM " + afSrc + ". SPAN = " + Span( off, stop ))
               while( off < stop ) {
                  val chunkLen = math.min( stop - off, 8192 ).toInt
                  afSrc.read( bufSrc, off, chunkLen )
                  afTgt.write( bufSrc, 0, chunkLen )
                  off += chunkLen
               }

               // fadein
               val lim     = limiter()
               val fSrcOut = outFader( 0L, fadeIn )
               val fOvrIn  = inFader( 0L, fadeIn )
               val rampOvr = ramp( 0L, overSpan.length, boostIn, boostOut )
               stop       += fadeIn
               var ovrOff  = overSpan.start
//               var ovrOff2 = overSpan.start

if( verbose ) println( "FADEIN FROM " + afOvr + ". SPAN = " + Span( ovrOff, ovrOff + (stop - off) ))
               while( off < stop ) {
                  val chunkLen = math.min( stop - off, 8192 ).toInt
                  readOvr( ovrOff, chunkLen )
                  rampOvr.process( fBufOvr, 0, fBufSrc, 0, chunkLen )
                  fOvrIn.process( fBufOvr, 0, fBufSrc, 0, chunkLen )
                  val chunkLen2  = lim.process( fBufOvr, 0, fBufTgt, 0, chunkLen )
                  afSrc.read( bufSrc, off, chunkLen2 )
                  fSrcOut.process( fBufSrc, 0, fBufSrc, 0, chunkLen2 )
                  add( fBufSrc, 0, fBufTgt, 0, chunkLen2 )
                  afTgt.write( bufTgt, 0, chunkLen2 )
                  ovrOff   += chunkLen
                  off      += chunkLen2
//                  ovrOff2  += chunkLen2
               }

               // over
               // off = sourceSpan.start + fadeIn
               off   = 0
               stop  = overSpan.length - (fadeIn + fadeOut)
if( verbose ) println( "OVERWRITING. SPAN = " + Span( ovrOff, ovrOff + (stop - off) ))
               while( off < stop ) {
                  val chunkLen   = math.min( stop - off, 8192 ).toInt
                  readOvr( ovrOff, chunkLen )
                  rampOvr.process( fBufOvr, 0, fBufSrc, 0, chunkLen )
                  val chunkLen2 = lim.process( fBufOvr, 0, fBufTgt, 0, chunkLen )
                  afTgt.write( bufTgt, 0, chunkLen2 )
                  ovrOff   += chunkLen
                  off      += chunkLen2
               }

               // fadeout
               val fSrcIn  = inFader( 0L, fadeOut )
               val fOvrOut = outFader( 0L, fadeOut )
               off   = sourceSpan.stop - fadeOut
               stop  = off + fadeOut
if( verbose ) println( "FADEOUT. SPAN (SRC) = " + Span( off, stop ))
               while( off < stop ) {
                  val chunkLen = math.min( stop - off, 8192 ).toInt
                  readOvr( ovrOff, chunkLen )
                  rampOvr.process( fBufOvr, 0, fBufSrc, 0, chunkLen )
                  fOvrOut.process( fBufOvr, 0, fBufSrc, 0, chunkLen )
                  val chunkLen2 = lim.process( fBufOvr, 0, fBufTgt, 0, chunkLen )
                  afSrc.read( bufSrc, off, chunkLen2 )
                  fSrcIn.process( fBufSrc, 0, fBufSrc, 0, chunkLen2 )
                  add( fBufSrc, 0, fBufTgt, 0, chunkLen2 )
                  afTgt.write( bufTgt, 0, chunkLen2 )
                  ovrOff   += chunkLen
                  off      += chunkLen2
               }

               // post
               stop  = phraseLen
if( verbose ) println( "COPY POST TO " + fTgt + ". SPAN (SRC) = " + Span( off, stop ))
               while( off < stop ) {
                  val chunkLen = math.min( stop - off, 8192 ).toInt
                  afSrc.read( bufSrc, off, chunkLen )
                  afTgt.write( bufSrc, 0, chunkLen )
                  off += chunkLen
               }
//               Phrase.fromFile( fTgt )
//               fTgt
               atomic( "AbstractDifferanceOverwriter phrase from file" ) { implicit tx =>
                  Phrase.fromFile( fTgt )
               }

            } finally {
               afTgt.close()
            }
         } finally {
            afOvr.close()
         }
      } finally {
         afSrc.close()
      }
   }
}