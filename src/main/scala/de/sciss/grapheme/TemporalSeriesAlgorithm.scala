package de.sciss.grapheme

trait TemporalSeriesAlgorithm {
   def step( implicit tx: Tx ) : Unit
}