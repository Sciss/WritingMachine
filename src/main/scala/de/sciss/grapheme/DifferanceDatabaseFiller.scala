package de.sciss.grapheme

trait DifferanceDatabaseFiller {
   def perform( implicit tx: Tx ) : Unit
}