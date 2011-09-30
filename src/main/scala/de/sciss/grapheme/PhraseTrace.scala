package de.sciss.grapheme

trait PhraseTrace {
   def add( phrase: Phrase )( implicit txn: Tx ) : Unit
   def series( n: Int )( implicit txn: Tx ) : IndexedSeq[ Phrase ]
}