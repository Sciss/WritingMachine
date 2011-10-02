package de.sciss.grapheme
package impl

import java.io.{FileFilter, File}
import de.sciss.synth.io.AudioFile
import de.sciss.synth.proc.Ref

object DatabaseImpl {
   def apply( dir: File )( implicit tx: Tx ) : Database = {
      val files = dir.listFiles( new FileFilter {
         def accept( f: File ) : Boolean = AudioFile.identify( f ).isDefined
      }).toList.sortBy( _.lastModified() )

      // left-overs from aborted apps
      files.drop( 1 ).foreach( _.delete() )

      files.headOption match {
         case Some( f ) =>
            new DatabaseImpl( AudioFile.openRead( f ))
         case None =>
      }

      sys.error( "TODO" )
   }
}
class DatabaseImpl private ( firstFile: AudioFile ) extends Database {
   private val fileRef = Ref( firstFile )
}