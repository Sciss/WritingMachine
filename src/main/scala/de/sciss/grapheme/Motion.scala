/*
 *  Motion.scala
 *  (WritingMachine)
 *
 *  Copyright (c) 2011-2017 Hanns Holger Rutz. All rights reserved.
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

import impl.{MotionImpl => Impl}

object Motion {
   def constant( value: Double ) : Motion = Impl.constant( value )
   def linrand( lo: Double, hi: Double ) : Motion = Impl.linrand( lo, hi )
   def exprand( lo: Double, hi: Double ) : Motion = Impl.exprand( lo, hi )
   def sine( lo: Double, hi: Double, period: Int ) : Motion = Impl.sine( lo, hi, period )
   def walk( lo: Double, hi: Double, maxStep: Double ) : Motion = Impl.walk( lo, hi, maxStep )

   def linlin( in: Motion, inLo: Double, inHi: Double, outLo: Double, outHi: Double ) : Motion =
      Impl.linlin( in, inLo, inHi, outLo, outHi )

   def linexp( in: Motion, inLo: Double, inHi: Double, outLo: Double, outHi: Double ) : Motion =
      Impl.linexp( in, inLo, inHi, outLo, outHi )

   def coin( prob: Double, a: Motion, b: Motion ) : Motion = Impl.coin( prob, a, b )
}
trait Motion {
   def step( implicit tx: Tx ) : Double
}