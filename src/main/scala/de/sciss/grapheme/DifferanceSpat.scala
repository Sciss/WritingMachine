package de.sciss.grapheme

trait DifferanceSpat {
   def rotateAndProject( phrase: Phrase )( implicit tx: Tx ) : Unit
}