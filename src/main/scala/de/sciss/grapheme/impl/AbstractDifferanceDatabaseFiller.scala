package de.sciss.grapheme
package impl

abstract class AbstractDifferanceDatabaseFiller extends DifferanceDatabaseFiller with GraphemeUtil {
   /**
    * Target database length in seconds.
    */
   def durationMotion : Motion

   def database : Database
   def television : Television
//   def fileSpace : FileSpace

   def perform( implicit tx: Tx ) {
      val length  = secondsToFrames( durationMotion.step )
//      val f       = fileSpace.newFile
//      val af      = openMonoWrite( f )
      val futDb   = television.capture( length )
   }
}