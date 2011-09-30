package de.sciss.grapheme

trait DifferanceDatabaseQuery {
   def find( phrase: Phrase, overwrite: OverwriteInstruction )( implicit tx: Tx ) : Span
}