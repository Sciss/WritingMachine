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