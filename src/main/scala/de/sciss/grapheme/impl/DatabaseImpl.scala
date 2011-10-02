package de.sciss.grapheme
package impl

import java.io.{FileFilter, File}
import de.sciss.synth.io.AudioFile
import de.sciss.synth.proc.Ref
import collection.immutable.{IndexedSeq => IIdxSeq}

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
            sys.error( "TODO" )
      }
   }
}
class DatabaseImpl private ( firstFile: AudioFile ) extends AbstractDatabase {
   val removalFadeMotion      = MotionImpl.exprand( 0.100, 1.000 )
   val removalSpectralMotion  = MotionImpl.linrand( 0.20, 0.80 )
   val removalMarginMotion    = MotionImpl.exprand( 0.250, 2.500 )

   def performRemovals( instrs: IIdxSeq[ RemovalInstruction ])( implicit tx: Tx ) {
      Console.err.println( "DatabaseImpl -> performRemovals : TODO" )
   }

   def bestRemoval( span: Span, margin: Long, weight: Double, fade: Long )( implicit tx: Tx ) : RemovalInstruction = {
      Console.err.println( "DatabaseImpl -> bestRemoval : TODO" )
      RemovalInstruction( span, fade, fade )
   }

   def append( af: AudioFile, length: Long )( implicit tx: Tx ) {
      Console.err.println( "DatabaseImpl -> append : TODO" )
   }

   def length : Long = {
      Console.err.println( "DatabaseImpl -> length : TODO" )
      0L
   }

   private val fileRef = Ref( firstFile )
}