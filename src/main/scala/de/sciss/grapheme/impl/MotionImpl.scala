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

object MotionImpl {
   def constant( value: Double ) : Motion = new Constant( value )
   def linrand( lo: Double, hi: Double ) : Motion = new LinRand( lo, hi )
   def exprand( lo: Double, hi: Double ) : Motion = new ExpRand( lo, hi )

   private final case class Constant( value: Double ) extends Motion {
      def step( implicit tx: Tx ) : Double = value
   }

   private final case class LinRand( lo: Double, hi: Double ) extends Motion with GraphemeUtil {
      val range = hi - lo
      def step( implicit tx: Tx ) : Double = random * range + lo
   }

   private final case class ExpRand( lo: Double, hi: Double ) extends Motion with GraphemeUtil {
      val factor = math.log( hi / lo )
      def step( implicit tx: Tx ) : Double = math.exp( random * factor ) * lo
   }
}