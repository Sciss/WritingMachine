package de.sciss.grapheme

trait Television {
   def capture( length: Long )( implicit tx: Tx ) : FutureResult[ Unit ]
}