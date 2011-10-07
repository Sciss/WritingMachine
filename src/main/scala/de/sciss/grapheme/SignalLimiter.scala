package de.sciss.grapheme

trait SignalLimiter {
   def process( in: Array[ Float ], inOff: Int, out: Array[ Float ], outOff: Int, len: Int ) : Int
   def flush( out: Array[ Float ], outOff: Int, len: Int ) : Int
}