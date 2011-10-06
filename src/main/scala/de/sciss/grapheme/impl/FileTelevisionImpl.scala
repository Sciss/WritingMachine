package de.sciss.grapheme
package impl

import java.io.File
import de.sciss.synth.io.{AudioFileSpec, AudioFile}

object FileTelevisionImpl {
   def apply( f: File ) : Television = {
      val spec = AudioFile.readSpec( f )
      require( spec.numChannels == 1, "Television is supposed to be mono" )
      require( spec.numFrames > 0, "Television file is empty" )
      new FileTelevisionImpl( f, spec )
   }
}
class FileTelevisionImpl private ( f: File, spec: AudioFileSpec ) extends Television with GraphemeUtil {
   def capture( length: Long )( implicit tx: Tx ) : FutureResult[ File ] = {
      threadFuture( "FileTelevisionImpl capture" ) {
         sys.error( "TODO" ) : File
      }
   }
}