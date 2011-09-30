package de.sciss.grapheme

trait Television {
   def capture( length: Long )( implicit tx: Tx ) : Unit
}