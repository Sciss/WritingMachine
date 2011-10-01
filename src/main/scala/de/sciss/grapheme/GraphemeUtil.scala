package de.sciss.grapheme

trait GraphemeUtil {
   def random( implicit tx: Tx ) : Double
   def secondsToFrames( sec: Double ) : Long
}