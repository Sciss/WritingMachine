package de.sciss.grapheme
package impl

import de.sciss.synth

abstract class DifferanceDatabaseQueryImpl extends DifferanceDatabaseQuery with GraphemeUtil {
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

   private def matchLength( implicit tx: Tx ) : Long = {
      import synth._
      val matchDur   = matchDeviationMotion.step
      val matchDev   = matchDeviationMotion.step
      val minFact    = 1.0 / (1 + matchDev)
      val maxFact    = 1 + matchDev
      val fact       = random.linexp( 0, 1, minFact, maxFact )
      secondsToFrames( fact * matchDur )
   }

   def find( phrase: Phrase, overwrite: OverwriteInstruction )( implicit tx: Tx ) : Span = {
      val inLen   = matchLength
      val outLen  = matchLength
      sys.error( "TODO" )
   }
}