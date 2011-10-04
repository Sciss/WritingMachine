package de.sciss.grapheme

trait DifferanceOverwriter {
   def perform( phrase: Phrase, source: OverwriteInstruction, target: DifferanceDatabaseQuery.Match )
              ( implicit tx: Tx ) : FutureResult[ Phrase ]
}