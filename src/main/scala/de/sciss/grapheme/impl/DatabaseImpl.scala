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
import de.sciss.synth.proc.Ref
import collection.immutable.{IndexedSeq => IIdxSeq}
import de.sciss.strugatzki.FeatureExtraction
import de.sciss.synth.io.{AudioFileSpec, AudioFile}

object DatabaseImpl {
   private val normName    = "feat_norms.aif"
   private val dirPrefix   = "database"
   private val xmlName     = "grapheme.xml"

   def apply( dir: File )( implicit tx: Tx ) : Database = {
      val normFile = new File( dir, normName )
      require( normFile.isFile, "Missing normalization file at " + normFile )

      val arr = dir.listFiles( new FileFilter {
         def accept( f: File ) = f.isDirectory && f.getName.startsWith( dirPrefix )
      })
      val subs = if( arr == null ) Nil else arr.toList

      // left-overs from aborted apps
      subs.drop( 1 ).foreach( deleteDir( _ ))

      val grapheme = subs.headOption.flatMap { sub =>
         val f = new File( sub, xmlName )
         if( f.isFile ) {
            try {
               val extr = FeatureExtraction.Settings.fromXMLFile( f )
               val spec = AudioFile.readSpec( extr.audioInput )
               Some( Entry( extr, spec ))
            } catch {
               case e =>
                  e.printStackTrace()
                  None
            }
         } else None
      }

      new DatabaseImpl( dir, normFile, grapheme )
   }

   /*
    * Assumes the directory is empty or contains only ordinary files.
    */
   private def deleteDir( dir: File ) : Boolean = {
      val arr  = dir.listFiles()
      val sub  = if( arr == null ) Nil else arr.toList
      sub.forall( _.delete() )
   }

   final case class Entry( extr: FeatureExtraction.Settings, spec: AudioFileSpec )
}
class DatabaseImpl private ( dir: File, normFile: File, grapheme0: Option[ DatabaseImpl.Entry ])
extends AbstractDatabase {
   import DatabaseImpl._

   private val graphemeRef    = Ref( grapheme0 )

   val removalFadeMotion      = Motion.exprand( 0.100, 1.000 )
   val removalSpectralMotion  = Motion.linrand( 0.20, 0.80 )
   val removalMarginMotion    = Motion.exprand( 0.250, 2.500 )

   def performRemovals( instrs: IIdxSeq[ RemovalInstruction ])( implicit tx: Tx ) : FutureResult[ Unit ] = {
      sys.error( "TODO" )
   }

   def bestRemoval( span: Span, margin: Long, weight: Double, fade: Long )( implicit tx: Tx ) : RemovalInstruction =
      sys.error( "TODO" )

   def append( f: File )( implicit tx: Tx ) : FutureResult[ Unit ] = sys.error( "TODO" )

   def length( implicit tx: Tx ) : Long = graphemeRef().map( _.spec.numFrames ).getOrElse( 0L )

   def asStrugatziDatabase( implicit tx: Tx ) : FutureResult[ File ] = sys.error( "TODO" )
}