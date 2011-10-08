/*
 *  PhraseImpl.scala
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
import synth.proc.{Proc, ProcFactory}

object PhraseImpl {
   import GraphemeUtil._

   private val identifier  = "phrase-impl"

   def fromFile( file: File )( implicit tx: Tx ) : Phrase = {
      import synth._
      import proc._
      import ugen._
      import DSL._

      val path       = file.getAbsolutePath
      val spec       = audioFileSpec( path )
      require( spec.numChannels == 1 )    // we need this now for the overwriter implementation!

      val factName   = "file-" + fileNameWithoutExtension( file ) // XXX hrmpfff
      val fact       = ProcDemiurg.factories.find( _.name == factName ).getOrElse {
         gen( factName ) {
            graph {
               val buf  = bufCue( path )
               val disk = DiskIn.ar( spec.numChannels, buf.id )
               // Done.kr( disk )
               val me   = Proc.local
               Done.kr( Line.kr( dur = spec.numFrames.toDouble / SampleRate.ir )).react {
                  threadFuture( identifier + " : spawn stop" ) {
                     atomic( identifier + " : stop proc" ) { implicit tx => me.stop }
                  }
                  ()
               }
               Mix.mono( disk )
            }
         }
      }

      new Impl( file, fact, spec.numFrames )
   }

   private final class Impl( file: File, fact: ProcFactory, val length: Long ) extends Phrase with ExtractionImpl {
      import GraphemeUtil._

      override def toString = "Phrase.fromFile(" + file + ")"

      def printFormat : String = {
         "phrase( " + fileNameWithoutExtension( file ) + ", " + formatSeconds( framesToSeconds( length )) + " )"
      }

      private val featureRef = Ref( Option.empty[ File ])

      def player( implicit tx: Tx ) : Proc = fact.make

      def asStrugatzkiInput( implicit tx: Tx ) : FutureResult[ File ] = featureRef() match {
         case Some( res ) => futureOf( res )
         case None =>
            extract( file, None ).map { res =>
               atomic( identifier + " : cache feature extraction" ) { implicit tx =>
                  featureRef.set( Some( res ))
               }
               res
            }
      }

      def reader( implicit tx: Tx ) : FrameReader.Factory = FrameReader.Factory( file )
   }
}