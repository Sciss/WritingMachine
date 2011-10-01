package de.sciss.grapheme

trait GraphemeUtil {
   protected def random( implicit tx: Tx ) : Double
   protected def random( top: Int )( implicit tx: Tx ) : Int
   protected def secondsToFrames( sec: Double ) : Long
   protected def max( i: Int, is: Int* ) : Int = is.foldLeft( i )( _ max _ )
   protected def max( n: Long, ns: Long* ) : Long = ns.foldLeft( n )( _ max _ )
   protected def max( d: Double, ds: Double* ) : Double = ds.foldLeft( d )( _ max _ )
   protected def min( i: Int, is: Int* ) : Int = is.foldLeft( i )( _ min _ )
   protected def min( n: Long, ns: Long* ) : Long = ns.foldLeft( n )( _ min _ )
   protected def min( d: Double, ds: Double* ) : Double = ds.foldLeft( d )( _ min _ )
}