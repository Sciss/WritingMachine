/*
 *  AbstractDifferanceDatabaseQuery.scala
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

import de.sciss.synth

abstract class AbstractDifferanceDatabaseQuery extends DifferanceDatabaseQuery with GraphemeUtil {
   import DifferanceDatabaseQuery._

   /**
    * Approximate duration of the cross-correlation in seconds.
    */
   def matchDurationMotion : Motion

   /**
    * Maximum allowed deviation from match duration (factor-offset,
    * e.g. 0 = no deviation allowed, 0.5 = 50% deviation allowed).
    */
   def matchDeviationMotion : Motion

   /**
    * Spectral weight in strugatzki (0 = temporal breaks, 1 = spectral breaks,
    * 0.5 = mix of both).
    */
   def spectralMotion : Motion

   /**
    * Maximum allowed deviation from target duration (factor-offset,
    * e.g. 0 = no deviation allowed, 0.5 = 50% deviation allowed).
    */
   def stretchDeviationMotion : Motion

   /**
    * Maximum deviation from number one match. (e.g. 0 = always take
    * best match, 1 = 50% best match and 50% second-best match,
    * 9 = 10% probability of choice among the 10 best matches)
    */
   def rankMotion : Motion

   private def matchLength( implicit tx: Tx ) : Long = {
      import synth._
      val matchDur   = matchDeviationMotion.step
      val matchDev   = matchDeviationMotion.step
      val minFact    = 1.0 / (1 + matchDev)
      val maxFact    = 1 + matchDev
      val fact       = random.linexp( 0, 1, minFact, maxFact )
      secondsToFrames( fact * matchDur )
   }

   def findMatch( rank: Int, phrase: Phrase, punchIn: Span, punchOut: Span,
                  minPunch: Long, maxPunch: Long, weight: Double )( implicit tx: Tx ) : FutureResult[ Match ]

   def find( phrase: Phrase, overwrite: OverwriteInstruction )( implicit tx: Tx ) : FutureResult[ Match ] = {
      val spect      = spectralMotion.step

      val stretchDev = stretchDeviationMotion.step
      val minConstr  = secondsToFrames( 0.1 )
      val minPunch   = max( minConstr, min( phrase.length/2, (overwrite.newLength / (1 + stretchDev)).toLong ))
      val maxPunch   = max( minConstr, min( phrase.length/2, (overwrite.newLength * (1 + stretchDev)).toLong ))

      val inLen      = matchLength
      val outLen     = matchLength
      val inner      = (minPunch + 1)/2 + maxPunch/2
      val (piStop, poStart) = if( inner >= overwrite.span.length ) {
         (overwrite.span.start + (inLen + 1) /2,
          overwrite.span.stop - outLen/2)
      } else { // move punchIn to left and punchOut to right, but keep touching point at same ratio
         val r    = ((minPunch + 1)/2).toDouble / inner
         val mid  = (r * overwrite.span.length).toLong + overwrite.span.start
         (mid, mid)
      }
      val piStart    = max( 0L, piStop - inLen )
      val poStop     = min( phrase.length, poStart + outLen )

      val punchIn    = Span( piStart, piStop )
      val punchOut   = Span( poStart, poStop )

      val rank       = random( max( 0, rankMotion.step.toInt ) + 1 )

      findMatch( rank, phrase, punchIn, punchOut, minPunch, maxPunch, spect )
   }
}