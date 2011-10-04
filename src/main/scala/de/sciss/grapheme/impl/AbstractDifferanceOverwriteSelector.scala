/*
 *  AbstractDifferanceOverwriteSelector.scala
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
import collection.immutable.{IndexedSeq => IIdxSeq}

abstract class AbstractDifferanceOverwriteSelector
extends DifferanceOverwriteSelector with GraphemeUtil {

   /**
    * Temporal stretch factor (1 = preserve duration, <1 = shorten, >1 = elongate).
    */
   def stretchMotion : Motion

   /**
    * Spectral weight in strugatzki (0 = temporal breaks, 1 = spectral breaks,
    * 0.5 = mix of both).
    */
   def spectralMotion : Motion

   /**
    * Approximate duration of fragments in seconds.
    */
   def fragmentDurationMotion : Motion

   /**
    * Maximum allowed deviation from fragment duration (factor-offset,
    * e.g. 0 = no deviation allowed, 0.5 = 50% deviation allowed).
    */
   def fragmentDeviationMotion : Motion

   /**
    * Probability skew of fragment positions (power factor, thus
    * 1 = same probability at beginning and ending, >1 = more towards
    * beginning, <1 = more towards ending).
    */
   def positionMotion : Motion

   /**
    * Amount of parts selected per step.
    */
   def frequencyMotion : Motion

   def bestPart( phrase: Phrase, center: Long, minLen: Long, maxLen: Long, weight: Double ) : FutureResult[ Span ]

   def selectParts( phrase: Phrase )( implicit tx: Tx ) : FutureResult[ IIdxSeq[ OverwriteInstruction ]] = {
      import synth._

      val num        = frequencyMotion.step.toInt
      IIdxSeq.fill( num ) {
         val stretch = stretchMotion.step
         val spect   = spectralMotion.step
         val fragDur = fragmentDurationMotion.step
         val fragDev = fragmentDeviationMotion.step
         val minFrag = secondsToFrames( fragDur / (1 + fragDev) )
         val maxFrag = secondsToFrames( fragDur * (1 + fragDev) )

         val posPow  = positionMotion.step
         val pos     = random.pow( posPow ).linlin( 0, 1, 0, phrase.length ).toLong

         val futSpan = bestPart( phrase, pos, minFrag, maxFrag, spect )
         futSpan map { span =>
            val newLen  = (span.length * stretch).toLong
            OverwriteInstruction( span, newLen )
         }
      }
   }
}