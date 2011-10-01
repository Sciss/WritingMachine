package de.sciss.grapheme

trait Motion {
   def step( implicit tx: Tx ) : Double
}