/*
 *  DatabaseImpl.scala
 *  (WritingMachine)
 *
 *  Copyright (c) 2011 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either
 *  version 2, june 1991 of the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License (gpl.txt) along with this software; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

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
   val removalFadeMotion      = Motion.exprand( 0.100, 1.000 )
   val removalSpectralMotion  = Motion.linrand( 0.20, 0.80 )
   val removalMarginMotion    = Motion.exprand( 0.250, 2.500 )

   def performRemovals( instrs: IIdxSeq[ RemovalInstruction ])( implicit tx: Tx ) {
      sys.error( "TODO" )
   }

   def bestRemoval( span: Span, margin: Long, weight: Double, fade: Long )( implicit tx: Tx ) : RemovalInstruction =
      sys.error( "TODO" )

   def append( af: AudioFile, length: Long )( implicit tx: Tx ) { sys.error( "TODO" )}

   def length : Long = sys.error( "TODO" )

   def asStrugatziDatabase( implicit tx: Tx ) : FutureResult[ File ] = sys.error( "TODO" )

   private val fileRef = Ref( firstFile )
}