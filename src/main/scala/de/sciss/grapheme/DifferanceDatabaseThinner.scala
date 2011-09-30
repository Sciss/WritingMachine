package de.sciss.grapheme

trait DifferanceDatabaseThinner {
   def remove( span: Span )( implicit tx: Tx ) : Unit
}