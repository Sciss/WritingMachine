package de.sciss.grapheme
package impl

object MotionImpl {
   def constant( value: Double ) : Motion = new Constant( value )

   private final case class Constant( value: Double ) extends Motion {
      def step( implicit tx: Tx ) : Double = value
   }
}