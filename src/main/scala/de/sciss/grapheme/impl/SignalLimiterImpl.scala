/*
 *  SignalLimiterImpl.scala
 *  (WritingMachine)
 *
 *  Copyright (c) 2011 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either
 *  version 2, june 1991 of the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License (gpl.txt) along with this software; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.grapheme
package impl

/**
 * A more or less literal translation of SuperCollider's `Limiter` UGen.
 */
object SignalLimiterImpl {
   def apply( lookAhead: Int, ceil: Float = 0.977f ) : SignalLimiter = new SignalLimiterImpl( lookAhead, ceil )
}
final class SignalLimiterImpl private ( lookAhead: Int, ceil: Float ) extends SignalLimiter {
   private var flips          = 0
   private var pos            = 0
   private var slope          = 0.0f
   private var level          = 1.0f
   private var prevmaxval     = 0.0f
   private var curmaxval      = 0.0f
   private val slopefactor    = 1.0f / lookAhead

   private val xBuf           = new Array[ Float ]( lookAhead * 3 /* 4 */)
   private var xInOff         = 0
   private var xMidOff        = lookAhead
   private var xOutOff        = lookAhead * 2
//   private val xFlushOff      = lookAhead * 4

   def process( in: Array[ Float ], inOff: Int, out: Array[ Float ], outOff: Int, len: Int ) : Int = {
      var remain     = len
      var buf_remain = lookAhead - pos
      var inPos      = inOff
      var outPos     = outOff

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

      outPos - outOff
   }

   def latency : Int = lookAhead * 2

//   def flush( out: Array[ Float ], outOff: Int, len: Int ) : Int = {
//      var remain  = len
//      var outPos  = outOff
//      while( remain > 0 ) {
//         val nsmps   = math.min( lookAhead, remain )
//         val pr      = process( xBuf, xFlushOff, out, outPos, nsmps )
//         outPos += pr
//         remain -= pr
//      }
//
//      outPos - outOff
//   }
}