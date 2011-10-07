package de.sciss.grapheme

import de.sciss.synth.io.Frames
import impl.{FrameReaderImpl => Impl}
import java.io.File

object FrameReader {
   def apply( file: File ) : FrameReader = Impl( file )
}
trait FrameReader {
   def read( buf: Frames, off: Long, len: Int ) : Unit
   def close() : Unit
}