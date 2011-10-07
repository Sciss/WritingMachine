package de.sciss.grapheme

object DSP {
   def add( in: Array[ Float ], inOff: Int, out: Array[ Float ], outOff: Int, len: Int ) {
      var i = 0
      while( i < len ) {
         out( outOff + i ) += in( inOff + i )
         i += 1
      }
   }

   def clear( in: Array[ Float ], inOff: Int, len: Int ) {
      var i = 0
      while( i < len ) {
         in( inOff + i ) = 0f
         i += 1
      }
   }
}