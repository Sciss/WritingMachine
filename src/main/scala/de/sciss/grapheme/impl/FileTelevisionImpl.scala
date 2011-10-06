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
class FileTelevisionImpl private ( f: File, spec: AudioFileSpec ) extends Television {
   import GraphemeUtil._

   def capture( length: Long )( implicit tx: Tx ) : FutureResult[ File ] = {
      threadFuture( "FileTelevisionImpl capture" ) {
         val fNew = createTempFile( ".aif" )

         sys.error( "TODO" ) : File
      }
   }
}