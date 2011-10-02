package de.sciss.grapheme

import collection.immutable.{IndexedSeq => IIdxSeq}

trait DifferanceOverwriteSelector {
   def selectParts( phrase: Phrase )( implicit tx: Tx ) : IIdxSeq[ OverwriteInstruction ]
}