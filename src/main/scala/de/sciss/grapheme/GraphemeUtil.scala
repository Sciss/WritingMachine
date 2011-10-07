/*
 *  GraphemeUtil.scala
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

import java.io.File
import de.sciss.synth.io.{SampleFormat, AudioFileType, AudioFileSpec, AudioFile}

object GraphemeUtil {
   var logTransactions  = true
   /**
    * Important: there seem to be problems with communications between
    * WritingMachine and scsynth when using `sys.props( "java.io.tmpdir" )`
    * which gives you something terrible as `/var/folders/M9/M9W+ucwpFzaqGpC2mu3KZq0sUCc/-Tmp-/`
    */
   var tmpDir           = new File( "/tmp" )

   val seed = 1L
   private val rng = new util.Random( seed )

   val deleteTempFilesOnExit  = false

   def logNoTx( text: => String ) {
      if( logTransactions ) logPrint( text )
   }

   def logTx( text: => String )( implicit tx: Tx ) {
      if( logTransactions ) tx.afterCommit( _ => logPrint( text ))
   }

   private def logPrint( text: String ) {
      println( timeString() + " " + text )
   }

   def timeString() = (new java.util.Date()).toString

//   def random( implicit tx: Tx ) : Double = math.random // XXX
   def random( implicit tx: Tx ) : Double = rng.nextDouble()
   def sampleRate : Double = 44100.0

   def random( top: Int )( implicit tx: Tx ) : Int = (random * top).toInt
   def secondsToFrames( secs: Double ) : Long = (secs * sampleRate + 0.5).toLong
   def max( i: Int, is: Int* ) : Int = is.foldLeft( i )( _ max _ )
   def max( n: Long, ns: Long* ) : Long = ns.foldLeft( n )( _ max _ )
   def max( d: Double, ds: Double* ) : Double = ds.foldLeft( d )( _ max _ )
   def min( i: Int, is: Int* ) : Int = is.foldLeft( i )( _ min _ )
   def min( n: Long, ns: Long* ) : Long = ns.foldLeft( n )( _ min _ )
   def min( d: Double, ds: Double* ) : Double = ds.foldLeft( d )( _ min _ )

   def openMonoWrite( f: File ) : AudioFile =
      AudioFile.openWrite( f, AudioFileSpec( AudioFileType.AIFF, SampleFormat.Float, 1, sampleRate ))

   def strugatzkiDatabase = WritingMachine.strugatzkiDatabase

   def createTempFile( suffix: String, dir: Option[ File ]) : File = {
      val res = File.createTempFile( "grapheme", suffix, dir.getOrElse( tmpDir ))
      if( deleteTempFilesOnExit ) res.deleteOnExit()
      res
   }

   def createDir( parent: File, prefix: String ) : File = {
      val f = File.createTempFile( prefix, "", parent )
      f.delete()
      require( f.mkdir(), "Could not create directory : " + f )
      f
   }

   def futureOf[ A ]( value: A ) : FutureResult[ A ] = FutureResult.now( value )

   def threadFuture[ A ]( name: String )( code: => A )( implicit tx: Tx ) : FutureResult[ A ] = {
      val ev = FutureResult.event[ A ]()
      tx.afterCommit { _ =>
         new Thread( name ) {
            start()
            override def run() {
               logNoTx( "threadFuture started : " + name )
               ev.set( code )
            }
         }
      }
      ev
   }
}