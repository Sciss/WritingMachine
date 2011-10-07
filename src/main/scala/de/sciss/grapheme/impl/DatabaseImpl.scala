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

      val (spec, extr) = subs.headOption match {
         case Some( sub ) =>
            val fExtr   = new File( sub, xmlName )
            val fNorm   = new File( sub, normName )
            if( fExtr.isFile && fNorm.isFile ) {
               try {
                  val extr       = FeatureExtraction.Settings.fromXMLFile( fExtr )
                  val spec       = AudioFile.readSpec( extr.audioInput )
                  val normSpec   = AudioFile.readSpec( fNorm )
                  if( normSpec.numFrames == 2 ) {  // make sure this is there and was fully written
                     (Some( (extr.audioInput, spec) ), Some( fExtr ))
                  } else {
                     (None, None)
                  }
               } catch {
                  case e =>
                     e.printStackTrace()
                     (None, None)
               }
            } else (None, None)

         case None => (None, None)
      }

      new DatabaseImpl( dir, normFile, State( spec, extr ))
   }

   /*
    * Assumes the directory is empty or contains only ordinary files.
    */
   private def deleteDir( dir: File ) : Boolean = {
      val arr  = dir.listFiles()
      val sub  = if( arr == null ) Nil else arr.toList
      sub.forall( _.delete() ) && dir.delete()
   }

   private def copyAudioFile( source: File, target: File )( implicit tx: Tx ) : FutureResult[ Unit ] = {
      import GraphemeUtil._

      threadFuture( "DatabaseImpl copy file " + source ) {
         val afSrc = AudioFile.openRead( source )
         try {
            val afTgt = AudioFile.openWrite( target, afSrc.spec )
            try {
               afSrc.copyTo( afTgt, afSrc.numFrames )
            } finally {
               afTgt.close()
            }
         } finally {
            afSrc.close()
         }
      }
   }

//   final case class Entry( extr: FeatureExtraction.Settings, spec: AudioFileSpec )
   final case class State( spec: Option[ (File, AudioFileSpec) ], extr: Option[ File ])
}
class DatabaseImpl private ( dir: File, normFile: File, state0: DatabaseImpl.State )
extends AbstractDatabase with ExtractionImpl {
   import GraphemeUtil._
   import DatabaseImpl._

//   private val extrRef        = Ref( extr0 )
//   private val specRef        = Ref( spec0 )
   private val stateRef       = Ref( state0 )

//   private val folderRef      = Ref( grapheme0.flatMap( _.extr.metaOutput ).map( _.getParentFile )
//      .getOrElse( createDir( dir )))

//   val removalFadeMotion      = Motion.exprand( 0.100, 1.000 )
//   val removalSpectralMotion  = Motion.linrand( 0.20, 0.80 )
//   val removalMarginMotion    = Motion.exprand( 0.250, 2.500 )
//
//   def performRemovals( instrs: IIdxSeq[ RemovalInstruction ])( implicit tx: Tx ) : FutureResult[ Unit ] = {
//      sys.error( "TODO" )
//   }
//
//   def bestRemoval( span: Span, margin: Long, weight: Double, fade: Long )( implicit tx: Tx ) : RemovalInstruction =
//      sys.error( "TODO" )

   def append( appFile: File, offset: Long, length: Long )( implicit tx: Tx ) : FutureResult[ Unit ] = {
      val oldFileO  = stateRef().spec.map( _._1 )
      threadFuture( "Database append" ) {
         atomic( "Database append tx" ) { implicit tx =>
            appendBody( oldFileO, appFile, offset, length )
         }
      }
   }

   def remove( instrs: IIdxSeq[ RemovalInstruction ])( implicit tx: Tx ) : FutureResult[ Unit ] = {
      val len        = length
      val filtered   = instrs.filter( i => i.span.nonEmpty && i.span.start >= 0 && i.span.stop <= len )
      val sorted     = filtered.sortBy( _.span.start )

      val merged     = {
         var pred       = RemovalInstruction( Span( -1L, -1L ), 0L, 0L )
         var res        = IIdxSeq.empty[ RemovalInstruction ]
         sorted.foreach { succ =>
            pred.merge( succ ) match {
               case Some( m ) =>
                  pred = m
               case None =>
                  res :+= pred
                  pred = succ
            }
         }
         if( pred.span.nonEmpty ) res :+= pred
         res
      }

      val oldFileO  = stateRef().spec.map( _._1 )

      threadFuture( "DatabaseImpl remove" ) {
         removalBody( oldFileO, merged )
      }
   }

   private def removalBody( oldFileO: Option[ File ], entries: IIdxSeq[ RemovalInstruction ]) {
//      try {
//         val sub     = createDir( dir, dirPrefix )
//         val fNew    = new File( sub, audioName )
//         val afNew   = AudioFile.openWrite( fNew,
//            AudioFileSpec( AudioFileType.AIFF, SampleFormat.Float, afApp.numChannels, afApp.sampleRate ))
//         try {
//            oldFileO.foreach { fOld =>
//               val afOld   = AudioFile.openRead( fOld )
//               try {
//                  require( afOld.numChannels == afApp.numChannels, "Database append - channel mismatch" )
//                  afOld.copyTo( afNew, afOld.numFrames )
//               } finally {
//                  afOld.close()
//               }
//            }
//            afApp.copyTo( afNew, length )
//            val oldState   = stateRef.swap( State( Some( (fNew, afNew.spec) ), None ))
//            val oldFileO2  = oldState.spec.map( _._1 )
//            tx.afterCommit { _ =>
//               oldFileO2.foreach { fOld2 =>
//                  deleteDir( fOld2.getParentFile )
//               }
//            }
//         } finally {
//            afNew.close()
//         }
//
//      } catch {
//         case e =>
//            println( "Database removal - Ooops, should handle exceptions" )
//            e.printStackTrace()
//      }
   }

   private def appendBody( oldFileO: Option[ File ], appFile: File, offset: Long, len: Long ) {
      import DSP._

      try {
         val sub     = createDir( dir, dirPrefix )
         val fNew    = new File( sub, audioName )
         val afApp   = AudioFile.openRead( appFile )
         try {
            val afNew   = AudioFile.openWrite( fNew,
               AudioFileSpec( AudioFileType.AIFF, SampleFormat.Float, afApp.numChannels, afApp.sampleRate ))
            try {
               afApp.seek( offset )
               val fdLen = oldFileO.map( fOld => {
                  val afOld   = AudioFile.openRead( fOld )
                  try {
                     require( afOld.numChannels == afApp.numChannels, "Database append - channel mismatch" )
                     val _fdLen = min( afOld.numFrames, len, secondsToFrames( 0.1 )).toInt
                     afOld.copyTo( afNew, afOld.numFrames - _fdLen )
                     if( _fdLen > 0 ) {
                        val fo      = SignalFader( 0L, _fdLen, 1f, 0f )  // make it linear, so we don't need a limiter
                        val foBuf   = afOld.buffer( _fdLen )
                        afOld.read( foBuf )
                        fo.process( foBuf( 0 ), 0, foBuf( 0 ), 0, _fdLen )
                        val fi      = SignalFader( 0L, _fdLen, 0f, 1f )
                        val fiBuf   = afApp.buffer( _fdLen )
                        afApp.read( fiBuf )
                        fi.process( fiBuf( 0 ), 0, fiBuf( 0 ), 0, _fdLen )
                        add( fiBuf( 0 ), 0, foBuf( 0 ), 0, _fdLen )
                        afNew.write( fiBuf )
                     }
                     _fdLen
                  } finally {
                     afOld.close()
                  }
               }).getOrElse( 0 )
               afApp.copyTo( afNew, len - fdLen )
               afNew.close()
               atomic( "Database append finalize" ) { tx =>
                  val oldState   = stateRef.swap( State( Some( (fNew, afNew.spec) ), None ))( tx )
                  val oldFileO2  = oldState.spec.map( _._1 )
                  tx.afterCommit { _ =>
                     oldFileO2.foreach { fOld2 =>
                        deleteDir( fOld2.getParentFile )
                     }
                  }
               }
            } finally {
               if( afNew.isOpen ) afNew.close()
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

   def length( implicit tx: Tx ) : Long = stateRef().spec.map( _._2.numFrames ).getOrElse( 0L )

   def reader( implicit tx: Tx ) : FrameReader.Factory = {
      val f = stateRef().spec.map( _._1 ).getOrElse( sys.error( "Database contains no file" ))
      FrameReader.Factory( f )
   }

   def asStrugatziDatabase( implicit tx: Tx ) : FutureResult[ File ] = {
      val state = stateRef()
      state.extr match {
         case Some( meta ) => futureOf( meta.getParentFile )
         case None =>
            val audioInput = state.spec.map( _._1 ).getOrElse( sys.error( "Database contains no file" ))
            val sub        = audioInput.getParentFile
            copyAudioFile( normFile, new File( sub, normName )).flatMap { _ =>
               extractAndUpdate( audioInput, sub )
            }
      }
   }

   private def extractAndUpdate( audioInput: File, sub: File ) : FutureResult[ File ] = {
      atomic( "DatabaseImpl extract" ) { implicit tx =>
         extract( audioInput, Some( sub )).map { meta =>
            assert( meta.getParentFile == sub )
            updateState( meta )
            sub
         }
      }
   }

   private def updateState( meta: File ) {
      atomic( "DatabaseImpl saving feature extraction cache" ) { implicit tx =>
         stateRef.transform( _.copy( extr = Some( meta )))
      }
   }
}