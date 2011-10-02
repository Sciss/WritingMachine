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

   def bestPart( phrase: Phrase, center: Long, minLen: Long, maxLen: Long, weight: Double ) : Span

   def selectParts( phrase: Phrase )( implicit tx: Tx ) : IIdxSeq[ OverwriteInstruction ] = {
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

         val span    = bestPart( phrase, pos, minFrag, maxFrag, spect )
         val newLen  = (span.length * stretch).toLong
         OverwriteInstruction( span, newLen )
      }
   }
}