package de.sciss.grapheme
package impl

import de.sciss.synth.proc.{DSL, Proc, Ref}

abstract class AbstractDifferanceSpat extends DifferanceSpat {
   def numChannels : Int
   def diffusion( chan: Int )( implicit tx: Tx ) : Proc

   private val chanRef = Ref( -1 )

   def rotateAndProject( phrase: Phrase )( implicit tx: Tx ) : FutureResult[ Unit ] = {
      val chan = (chanRef() + 1) % numChannels
      chanRef.set( chan )

      import DSL._
      val pProc   = phrase.player
      val pDiff   = diffusion( chan )
      pProc ~> pDiff
      pProc.play

      FutureResult.now( () )  // XXX TODO react to phrase having being played completely
   }
}