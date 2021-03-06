/*
 *  MotionImpl.scala
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

import de.sciss.synth

object MotionImpl {
   import GraphemeUtil._

   def constant( value: Double ) : Motion = Constant( value )
   def linrand( lo: Double, hi: Double ) : Motion = LinRand( lo, hi )
   def exprand( lo: Double, hi: Double ) : Motion = ExpRand( lo, hi )
   def sine( lo: Double, hi: Double, period: Int ) : Motion = Sine( lo, hi, period )
   def walk( lo: Double, hi: Double, maxStep: Double ) : Motion = Walk( lo, hi, maxStep )

   def linlin( in: Motion, inLo: Double, inHi: Double, outLo: Double, outHi: Double ) : Motion =
      LinLin( in, inLo, inHi, outLo, outHi )

   def linexp( in: Motion, inLo: Double, inHi: Double, outLo: Double, outHi: Double ) : Motion =
      LinExp( in, inLo, inHi, outLo, outHi )

   def coin( prob: Double, a: Motion, b: Motion ) : Motion = Coin( prob, a, b )

   private final case class Constant( value: Double ) extends Motion {
      def step( implicit tx: Tx ) : Double = value
   }

   private final case class LinRand( lo: Double, hi: Double ) extends Motion {
      val range = hi - lo
      def step( implicit tx: Tx ) : Double = random * range + lo
   }

   private final case class ExpRand( lo: Double, hi: Double ) extends Motion {
      val factor = math.log( hi / lo )
      def step( implicit tx: Tx ) : Double = math.exp( random * factor ) * lo
   }

   private final case class Walk( lo: Double, hi: Double, maxStep: Double ) extends Motion {
      val maxStep2   = maxStep * 2
      val current    = Ref( Double.NaN )
      def step( implicit tx: Tx ) : Double = {
         val c = current()
         val v = if( c.isNaN ) {
            random * (hi - lo) + lo
         } else {
            max( lo, min( hi, c + (random * maxStep2 - maxStep)))
         }
         current.set( v )
         v
      }
   }

   private final case class Sine( lo: Double, hi: Double, period: Int ) extends Motion {
      val phase   = Ref( 0 )
      val mul     = (hi - lo) / 2
      val add     = mul + lo
      val factor  = math.Pi * 2 / period
      def step( implicit tx: Tx ) : Double = {
         val p = phase()
         phase.set( (p + 1) % period )
         math.sin( p * factor ) * mul + add
      }
   }

   private final case class Coin( prob: Double, a: Motion, b: Motion )
   extends Motion {
      def step( implicit tx: Tx ) : Double = {
         val m = if( random >= prob ) a else b
         m.step
      }
   }

   private final case class LinLin( in: Motion, inLo: Double, inHi: Double, outLo: Double, outHi: Double )
   extends Motion {
      def step( implicit tx: Tx ) : Double = {
         import synth._
         in.step.linlin( inLo, inHi, outLo, outHi )
      }
   }

   private final case class LinExp( in: Motion, inLo: Double, inHi: Double, outLo: Double, outHi: Double )
   extends Motion {
      def step( implicit tx: Tx ) : Double = {
         import synth._
         in.step.linexp( inLo, inHi, outLo, outHi )
      }
   }
}