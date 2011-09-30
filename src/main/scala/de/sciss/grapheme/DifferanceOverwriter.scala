package de.sciss.grapheme

trait DifferanceOverwriter {
   def perform( phrase: Phrase, source: OverwriteInstruction, target: Span )( implicit tx: Tx ) : Phrase
}