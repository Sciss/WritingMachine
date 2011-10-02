package de.sciss.grapheme

import collection.immutable.{IndexedSeq => IIdxSeq}

trait DifferanceDatabaseThinner {
   def remove( spans: IIdxSeq[ Span ])( implicit tx: Tx ) : Unit
}