package de.sciss.grapheme

trait DifferanceAlgorithm {
   def step( implicit tx: Tx ) : Unit
}