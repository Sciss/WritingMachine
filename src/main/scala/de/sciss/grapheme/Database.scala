package de.sciss.grapheme

import collection.immutable.{IndexedSeq => IIdxSeq}

trait Database {
//   def randomPhrase( length: Long )( implicit tx: Tx ) : Phrase
   def remove( spans: IIdxSeq[ Span ])( implicit tx: Tx ) : Unit
}