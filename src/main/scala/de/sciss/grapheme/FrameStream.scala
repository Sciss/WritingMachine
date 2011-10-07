//package de.sciss.grapheme
//
//import java.io.File
//
//trait FrameStream {
//   def length( implicit tx: Tx ): Long
//
//   /**
//    * Returns a directory carrying the strugatzki meta files of
//    * the database.
//    */
//   def asStrugatziDatabase( implicit tx: Tx ) : FutureResult[ File ]
//
//   def reader() : FrameReader
//}