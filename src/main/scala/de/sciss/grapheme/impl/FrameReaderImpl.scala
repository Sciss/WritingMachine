package de.sciss.grapheme
package impl

import java.io.File
import de.sciss.synth.io.{AudioFile, Frames}

object FrameReaderImpl {
   def apply( file: File ) : FrameReader = {
      val af = AudioFile.openRead( file )
      new FrameReaderImpl( af )
   }
}
class FrameReaderImpl private ( af: AudioFile ) extends FrameReader {
   def read( buf: Frames, off: Long, len: Int ) {
      if( off != af.position ) { af.position = off }
      af.read( buf, 0, len )
   }

   def close() { af.close() }
}