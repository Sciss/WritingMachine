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

//object AbstractDifferanceOverwriter {
//   def apply() : DifferanceOverwriter = new AbstractDifferanceOverwriter()
//}
abstract class AbstractDifferanceOverwriter private () extends DifferanceOverwriter {
   import GraphemeUtil._

   /**
    * Cross-fade punch-in duration in seconds
    */
   def fadeInMotion : Motion
   /**
    * Cross-fade punch-out duration in seconds
    */
   def fadeOutMotion : Motion

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
         performBody( pReaderF, dbReaderF, pSpanFd, dbSpanFd, target.boostIn, target.boostOut, fadeIn, fadeOut )
      }
   }

   private def performBody( pReaderF: FrameReader.Factory, dbReaderF: FrameReader.Factory,
                            sourceSpan: Span, targetSpan: Span, boostIn: Float, boostOut: Float,
                            fadeIn: Long, fadeOut: Long ) : Phrase = {

      sys.error( "TODO" )
   }
}