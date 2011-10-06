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
import de.sciss.synth.io.{AudioFileType, SampleFormat, AudioFileSpec, AudioFile}

object DatabaseImpl {
   private val normName    = "feat_norms.aif"
   private val dirPrefix   = "database"
   private val xmlName     = "grapheme.xml"
   private val audioName   = "grapheme.aif"

   def apply( dir: File )( implicit tx: Tx ) : Database = {
      val normFile = new File( dir, normName )
      require( normFile.isFile, "Missing normalization file at " + normFile )

      val arr = dir.listFiles( new FileFilter {
         def accept( f: File ) = f.isDirectory && f.getName.startsWith( dirPrefix )
      })
      val subs = (if( arr == null ) Nil else arr.toList).sortBy( f => -f.lastModified() ) // newest first

      // left-overs from aborted apps
      subs.drop( 1 ).foreach( deleteDir( _ ))

      val (extr, spec ) = subs.headOption match {
         case Some( sub ) =>
            val f = new File( sub, xmlName )
            if( f.isFile ) {
               try {
                  val extr = FeatureExtraction.Settings.fromXMLFile( f )
                  val spec = AudioFile.readSpec( extr.audioInput )
                  (Some( extr ), Some( (extr.audioInput, spec) ))
               } catch {
                  case e =>
                     e.printStackTrace()
                     (None, None)
               }
            } else (None, None)

         case None => (None, None)
      }

      new DatabaseImpl( dir, normFile, extr, spec )
   }

   /*
    * Assumes the directory is empty or contains only ordinary files.
    */
   private def deleteDir( dir: File ) : Boolean = {
      val arr  = dir.listFiles()
      val sub  = if( arr == null ) Nil else arr.toList
      sub.forall( _.delete() ) && dir.delete()
   }

   private def createDir( parent: File ) : File = {
      val f = File.createTempFile( dirPrefix, "", parent )
      f.delete()
      require( f.mkdir(), "Could not create directory : " + f )
      f
   }

//   final case class Entry( extr: FeatureExtraction.Settings, spec: AudioFileSpec )
}
class DatabaseImpl private ( dir: File, normFile: File, extr0: Option[ FeatureExtraction.Settings ],
                             spec0: Option[ (File, AudioFileSpec) ])
extends AbstractDatabase {
   import GraphemeUtil._
   import DatabaseImpl._

   private val extrRef        = Ref( extr0 )
   private val specRef        = Ref( spec0 )
//   private val folderRef      = Ref( grapheme0.flatMap( _.extr.metaOutput ).map( _.getParentFile )
//      .getOrElse( createDir( dir )))

   val removalFadeMotion      = Motion.exprand( 0.100, 1.000 )
   val removalSpectralMotion  = Motion.linrand( 0.20, 0.80 )
   val removalMarginMotion    = Motion.exprand( 0.250, 2.500 )

   def performRemovals( instrs: IIdxSeq[ RemovalInstruction ])( implicit tx: Tx ) : FutureResult[ Unit ] = {
      sys.error( "TODO" )
   }

   def bestRemoval( span: Span, margin: Long, weight: Double, fade: Long )( implicit tx: Tx ) : RemovalInstruction =
      sys.error( "TODO" )

   def append( appFile: File, offset: Long, length: Long )( implicit tx: Tx ) : FutureResult[ Unit ] = {
      val oldFileO   = specRef().map( _._1 )
      threadFuture( "Database append" )( appendBody( oldFileO, appFile, offset, length ))
   }

   private def appendBody( oldFileO: Option[ File ], appFile: File, offset: Long, length: Long )( implicit tx: Tx ) {
      try {
         val sub     = createDir( dir )
         val fNew    = new File( sub, audioName )
         val afApp   = AudioFile.openRead( appFile )
         try {
            val afNew   = AudioFile.openWrite( fNew,
               AudioFileSpec( AudioFileType.AIFF, SampleFormat.Float, afApp.numChannels, afApp.sampleRate ))
            try {
               afApp.seek( offset )
               oldFileO.foreach { fOld =>
                  val afOld   = AudioFile.openRead( fOld )
                  try {
                     require( afOld.numChannels == afApp.numChannels, "Database append - channel mismatch" )
                     afOld.copyTo( afNew, afOld.numFrames )
                  } finally {
                     afOld.close()
                  }
               }
               afApp.copyTo( afNew, length )
               atomic( "Database append finalize" ) { tx1 =>
                  extrRef.set( None )( tx1 )
                  val oldFileO2 = specRef.swap( Some( (fNew, afNew.spec) ))( tx1 ).map( _._1 )
                  tx1.afterCommit { _ =>
                     oldFileO2.foreach { fOld2 =>
                        deleteDir( fOld2.getParentFile )
                     }
                  }
               }
            } finally {
               afNew.close()
            }
         } finally {
            afApp.close()
         }

      } catch {
         case e =>
            println( "Database append - Ooops, should handle exceptions" )
            e.printStackTrace()
      }
   }

   def length( implicit tx: Tx ) : Long = specRef().map( _._2.numFrames ).getOrElse( 0L )

   def asStrugatziDatabase( implicit tx: Tx ) : FutureResult[ File ] = {
      extrRef() match {
         case Some( extr ) => futureOf( extr.metaOutput.get.getParentFile ) // XXX Option.get not so sweet
         case None =>
            val tup = specRef().getOrElse( sys.error( "Database contains no file" ))
            performExtraction( tup._1 )
      }
   }

   private def performExtraction( audioInput: File ) : FutureResult[ File ] = {
      sys.error( "TODO" )
   }
}