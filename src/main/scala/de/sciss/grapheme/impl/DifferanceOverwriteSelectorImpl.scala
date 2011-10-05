/*
 *  DifferanceOverwriteSelectorImpl.scala
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

import de.sciss.synth.io.AudioFile
import de.sciss.strugatzki.{FeatureSegmentation, FeatureExtraction, Span => SSpan}
import collection.immutable.{IndexedSeq => IIdxSeq}
import java.io.File

object DifferanceOverwriteSelectorImpl {
   def apply() : DifferanceOverwriteSelector = new  DifferanceOverwriteSelectorImpl()
}
final class DifferanceOverwriteSelectorImpl () extends AbstractDifferanceOverwriteSelector {
   val stretchMotion             = Motion.linexp( Motion.sine( 0, 1, 30 ), 0, 1, 0.5, 2.0 )
   val fragmentDurationMotion    = Motion.exprand( 0.5, 4.0 )
   val fragmentDeviationMotion   = Motion.constant( 0.5 )
   val positionMotion            = Motion.constant( 1 )
   val frequencyMotion           = Motion.constant( 4 )
   val spectralMotion            = Motion.linrand( 0.25, 0.75 )

   /**
    * Length of correlation in seconds
    */
   val correlationMotion         = Motion.exprand( 0.250, 1.250 )

   val numBreaks                 = 200

   /**
    * Note that currently Strugatzki doesn't allow a 'punchOut' for segmentation.
    * Our algorithm thus proceeds as follows:
    *
    * - determine the correlation length from motion
    * - search span start is max( 0, center - maxLen/2 - corrLen )
    * - search span stop is min( fileLen, span start + maxLen + 2*corrLen )
    * - find one break
    * - return a span around that break (is possible covering center) for
    *   a random length between minLen and maxLen
    *
    * Previous, more complicated idea:
    *
    * - determine the correlation length from motion
    * - search span start is max( 0, center - maxLen/2 - corrLen )
    * - search span stop is min( fileLen, span start + maxLen + 2*corrLen )
    * - find an arbitrary number of breaks (e.g. 200)
    * - set min spacing to span length / num breaks
    * - with each break from the result:
    *   - go through the possible punch-out candidates (e.g. those that
    *     are spaced so that minLen and maxLen are met)
    *   - from the candidates keep track of the best choice (highest dissimilarity)
    */
   def bestPart( phrase: Phrase, center: Long, minLen: Long, maxLen: Long, weight: Double )
               ( implicit tx: Tx ) : FutureResult[ Span ] = {
      val strugFut = phrase.asStrugatzkiInput
      strugFut.flatMap { metaInput =>
         bestPartWith( metaInput, center, minLen, maxLen, weight )
      }
   }

   private def bestPartWith( metaInput: File, center: Long, minLen: Long, maxLen: Long,
                             weight: Double ) : FutureResult[ Span ] = {
//      atomic( "DifferanceOverwriteSelectorImpl : start strugatzki" ) { tx1 =>
         val res              = FutureResult.event[ Span ]()
//         tx1.afterCommit { _ =>
//println( "Juhuuu. Starting FeatureSegmentation for " + metaInput )
            val extr             = FeatureExtraction.Settings.fromXMLFile( metaInput )
            val spec             = AudioFile.readSpec( extr.audioInput )
            val set              = FeatureSegmentation.SettingsBuilder()
            set.databaseFolder   = strugatzkiDatabase
            set.metaInput        = metaInput
            val corrLen          = atomic( "DifferanceOverwriteSelectorImpl : correlationMotion" ) { tx1 =>
               secondsToFrames( correlationMotion.step( tx1 ))
            }
            val maxLenH          = maxLen / 2
            val start            = max( 0L, center - maxLenH - corrLen )
            val stop             = min( spec.numFrames, start + maxLen + corrLen + corrLen )
            set.span             = Some( SSpan( start, stop ))
            set.corrLen          = corrLen
            set.temporalWeight   = weight.toFloat
            set.normalize        = true
            set.numBreaks        = 2
            set.minSpacing       = 0
            val setb             = set.build
            val segm             = FeatureSegmentation( setb ) {
               case FeatureSegmentation.Aborted =>
                  println( "DifferanceOverwriteSelector : Ouch. Aborted. Need to handle this case!" )
                  res.set( Span( 0L, min( spec.numFrames, secondsToFrames( 1.0 ))))

               case FeatureSegmentation.Failure( e ) =>
                  println( "DifferanceOverwriteSelector : Ouch. Failure. Need to handle this case!" )
                  e.printStackTrace()
                  res.set( Span( 0L, min( spec.numFrames, secondsToFrames( 1.0 ))))

               case FeatureSegmentation.Success( coll ) =>
                  val b    = if( coll( 0 ).sim.isNaN ) coll( 1 ) else coll( 0 )
                  val len  = atomic( "DifferanceOverwriteSelector : success" ) { tx2 =>
                     (random( tx2 ) * (maxLen - minLen) + minLen).toLong
                  }
                  val s    = if( b.pos <= center ) {
                     val stop0   = max( center, b.pos + len/2 )
                     val start   = max( 0L, stop0 - len )
                     val stop    = min( spec.numFrames, start + len )
                     Span( start, stop )
                  } else {
                     val start0  = min( center, b.pos - len/2 )
                     val stop    = min( spec.numFrames, start0 + len )
                     val start   = max( 0L, stop - len )
                     Span( start, stop )
                  }
                  res.set( s )

               case FeatureSegmentation.Progress( p ) =>
            }
            segm.start()
//         }
         res
//      }
   }
}