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
extends DifferanceOverwriteSelector {
   import GraphemeUtil._

   private val identifier = "a-overwrite-selector"

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

   def bestPart( phrase: Phrase, center: Long, minLen: Long, maxLen: Long, weight: Double )
               ( implicit tx: Tx ) : FutureResult[ Span ]

   def selectParts( phrase: Phrase )( implicit tx: Tx ) : FutureResult[ IIdxSeq[ OverwriteInstruction ]] = {
      val num        = frequencyMotion.step.toInt
      val ovrNow     = futureOf( IIdxSeq.empty[ OverwriteInstruction ])
      selectPartsWith( ovrNow, phrase, num )
   }

   private def selectPartsWith( ovrNow: FutureResult[ IIdxSeq[ OverwriteInstruction ]], phrase: Phrase,
                                num : Int ) : FutureResult[ IIdxSeq[ OverwriteInstruction ]] = {
      (0 until num).foldLeft( ovrNow ) { case (futPred, i) =>
         futPred.flatMap { coll =>
            val (stretch, futSpan) = atomic( identifier + " : select parts" ) { tx1 =>
               import synth._

               val stre    = stretchMotion.step( tx1 )
               val spect   = spectralMotion.step( tx1 )
               val fragDur = fragmentDurationMotion.step( tx1 )
               val fragDev = fragmentDeviationMotion.step( tx1 )
               val minFrag = secondsToFrames( fragDur / (1 + fragDev) )
               val maxFrag = secondsToFrames( fragDur * (1 + fragDev) )

               val posPow  = positionMotion.step( tx1 )
               val pos     = random( tx1 ).pow( posPow ).linlin( 0, 1, 0, phrase.length ).toLong

               (stre, bestPart( phrase, pos, minFrag, maxFrag, spect )( tx1 ))
            }
            futSpan.map { span =>
               val newLen  = (span.length * stretch).toLong
               val ins     = OverwriteInstruction( span, newLen )
               coll :+ ins
            }
         }
      }
   }
}