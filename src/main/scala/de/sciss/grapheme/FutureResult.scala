package de.sciss.grapheme

import collection.immutable.{IndexedSeq => IIdxSeq}

trait FutureResult[ A ] {
   def map[ B ]( fun: A => B ) : FutureResult[ B ]
   def flatMap[ B ]( fun: A => FutureResult[ B ]) : FutureResult[ B ]
}

object FutureResult {
   def enrich[ A ]( f: IIdxSeq[ FutureResult[ A ]]) : FutureResult[ IIdxSeq[ A ]] = sys.error( "TODO" )
   def now[ A ]( value: A )( implicit tx: Tx ) : FutureResult[ A ] = sys.error( "TODO" )
   def unitSeq( fs: FutureResult[ _ ]* ) : FutureResult[ Unit ] = sys.error( "TODO" )
   def event[ A ]() : Event[ A ] = sys.error( "TODO" )

   trait Event[ A ] extends FutureResult[ A ] {
      def set( result: A ) : Unit
   }
}