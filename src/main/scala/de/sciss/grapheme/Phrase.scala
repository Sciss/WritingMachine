package de.sciss.grapheme

import de.sciss.synth.proc.Proc

trait Phrase {
   def length: Long
   def player( implicit tx: Tx ) : Proc

//   /**
//    * Plays this phrase into a given diffusion proc
//    * (by creating a gen proc and connecting its output
//    * to the diffusion). The result represents the
//    * phrase
//    */
//   def playInto( diffusion: Proc )( implicit tx: Tx ) : FutureResult[ Unit ]
}