package de.sciss.grapheme

trait DifferanceOverwriteSelector {
   def selectParts( phrase: Phrase )( implicit tx: Tx ) : Seq[ OverwriteInstruction ]
}