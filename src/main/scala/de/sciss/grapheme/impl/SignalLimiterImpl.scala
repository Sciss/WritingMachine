package de.sciss.grapheme
package impl

/**
 * A more or less literal translation of SuperCollider's `Limiter` UGen.
 */
object SignalLimiterImpl {
   def apply( lookAhead: Int, ceil: Float = 0.977f ) : SignalLimiter = new SignalLimiterImpl( lookAhead, ceil )
}
final class SignalLimiterImpl private ( lookAhead: Int, ceil: Float ) extends SignalLimiter {
//   allocsize = (m_bufsize * 3).nextPowerOfTwo
//   m_table = buffer of size allocsize

   private var flips          = 0
   private var pos            = 0
   private var slope          = 0.0f
   private var level          = 1.0f
   private var prevmaxval     = 0.0f
   private var curmaxval      = 0.0f
   private val slopefactor    = 1.0f / lookAhead

   private val xBuf           = new Array[ Float ]( lookAhead * 4 )
   private var xInOff         = 0
   private var xMidOff        = lookAhead
   private var xOutOff        = lookAhead * 2
   private val xFlushOff      = lookAhead * 4

//   private var latency        = lookAhead * 2

   def process( in: Array[ Float ], inOff: Int, out: Array[ Float ], outOff: Int, len: Int ) : Int = {
      var remain     = len
      var buf_remain = lookAhead - pos
      var inPos      = inOff
      var outPos     = outOff

//      val res        = math.max( 0, len - latency )   // number of samples written to out in this go

      while( remain > 0 ) {
         val nsmps   = math.min( remain, buf_remain )
         var xInPos  = xInOff + pos
         var xOutPos = xOutOff +  pos
         val stop    = inPos + nsmps
         if( flips >= 2 ) {
//assert( latency == 0 )
            while( inPos < stop ) {
               val v = in( inPos )
               inPos += 1
               xBuf( xInPos ) = v
               xInPos += 1
//               if( latency == 0 ) {
                  out( outPos ) = level * xBuf( xOutPos )
                  outPos += 1
//               } else {
//                  latency -= 1
//               }
               xOutPos += 1
               level += slope
               val va = math.abs( v )
               if( va > curmaxval ) curmaxval = va
            }
         } else {
            while( inPos < stop ) {
               val v = in( inPos )
               inPos += 1
               xBuf( xInPos ) = v
               xInPos += 1
//               out( outPos ) = 0.0f
//               outPos += 1
               level += slope
               val va = math.abs( v )
               if( va > curmaxval ) curmaxval = va
            }
         }

         pos += nsmps

         if( pos >= lookAhead ) {
            pos         = 0
            buf_remain  = lookAhead

            val maxval2  = math.max( prevmaxval, curmaxval )
            prevmaxval   = curmaxval
            curmaxval    = 0.0f

            val next_level = if( maxval2 > ceil ) {
               ceil / maxval2
            } else {
               1.0f
            }

            slope = (next_level - level) * slopefactor

            val temp    = xOutOff
            xOutOff     = xMidOff
            xMidOff     = xInOff
            xInOff      = temp

            flips += 1
         }
         remain -= nsmps
      }

//      res
      outPos - outOff
   }

   def flush( out: Array[ Float ], outOff: Int, len: Int ) : Int = {
      var remain  = len
      var outPos  = outOff
      while( remain > 0 ) {
         val nsmps   = math.min( lookAhead, remain )
         val pr      = process( xBuf, xFlushOff, out, outPos, nsmps )
         outPos += pr
         remain -= pr
      }

      outPos - outOff
   }
}