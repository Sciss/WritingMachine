package de.sciss.grapheme

trait MetaAlgorithm {
//   def differance: DifferanceAlgorithm
//   def temporalSeries: TemporalSeriesAlgorithm

   def step( implicit tx: Tx ) : Unit
}