/*
 *  LiveTelevisionImpl.scala
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

import java.io.File
import de.sciss.synth
import synth.io.{AudioFile, SampleFormat, AudioFileType}

object LiveTelevisionImpl {
   private val identifier  = "live-television-impl"

   def apply() : Television = new LiveTelevisionImpl()
}
final class LiveTelevisionImpl private () extends Television {
   import GraphemeUtil._
   import LiveTelevisionImpl._

   private val procRef = Ref( Option.empty[ synth.proc.Proc ])

   def capture( length: Long )( implicit tx: Tx ) : FutureResult[ File ] = {
      import synth._
      import ugen._
      import proc._
      import DSL._

      val dur = framesToSeconds( length )
      val res = FutureResult.event[ File ]()

      val p = procRef().getOrElse {
         val fact = diff( "$live-tv" ) {
            val pBoost  = pControl( "boost", ParamSpec( 1.0, 10.0, ExpWarp ), 1.0 )
            val pDur    = pScalar(  "dur",   ParamSpec( 0.0, 600.0 ), 10.0 )
            graph { in: In =>
               val path = createTempFile( ".aif", None )
               val mix  = Mix.mono( in ) * pBoost.kr
               val buf  = bufRecord( path.getAbsolutePath, 1, AudioFileType.AIFF, SampleFormat.Int24 )
               val dura = pDur.ir
               val me   = Proc.local
               Done.kr( Line.kr( dur = dura )).react {
                  thread( identifier + " : capture completed" ) {
                     atomic( identifier + " stop process" ) { implicit tx => me.stop }
                     var len = 0L
                     var i = 10
                     // some effort to make sure the file was closed by the server
                     // ; unfortunately we can't guarantee this with the current
                     // sound processes (or can we?)
                     while( len == 0L && i > 0 ) {
                        try {
                           val spec = AudioFile.readSpec( path )
                           len      = spec.numFrames
                        } catch {
                           case _ =>
                        }
                        if( len == 0L ) Thread.sleep( 200 )
                        i -= 1
                     }
                     atomic( identifier + " : return path" ) { implicit tx => res.set( path )}
                  }
               }
               DiskOut.ar( buf.id, mix )
            }
         }
         val _p = fact.make
         procRef.set( Some( _p ))
         _p.audioInput( "in" ).bus  = Some( RichBus.soundIn( Server.default,
            WritingMachine.tvNumChannels, WritingMachine.tvChannelOffset ))
         _p.control( "boost" ).v    = WritingMachine.tvBoostDB.dbamp
         _p.control( "dur" ).v      = dur
         _p
      }

      require( !p.isPlaying, identifier + " : still in previous capture" )

      res
   }
}