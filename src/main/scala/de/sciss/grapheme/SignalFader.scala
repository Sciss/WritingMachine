package de.sciss.grapheme

trait SignalFader {
   def process( in: Array[ Float ], inOff: Int, out: Array[ Float ], outOff: Int, len: Int ) : Unit
}