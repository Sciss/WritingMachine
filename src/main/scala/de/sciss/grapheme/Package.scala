package de.sciss

import synth.proc
import collection.immutable.{IndexedSeq => IIdxSeq}

package object grapheme {
   type Tx = proc.ProcTxn
//   type Future[ A ] = scala.actors.Future[ A ]
   type Ref[ A ] = proc.Ref[ A ]

   def Ref[ A : ClassManifest ]( init: A ) = proc.Ref( init )

   def atomic[ A ]( fun: Tx => A ) : A = {
      sys.error( "TODO" )
   }

   implicit def wrapFutureResultSeq[ A ]( fs: IIdxSeq[ FutureResult[ A ]]) : FutureResult[ IIdxSeq[ A ]] =
      FutureResult.enrich( fs )
}