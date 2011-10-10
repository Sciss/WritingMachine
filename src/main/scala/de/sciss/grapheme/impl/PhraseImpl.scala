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
            val pRelease = pControl( "release", ParamSpec( 0, 1, LinWarp, 1 ), 0 )
            graph {
               val buf     = bufCue( path )
//               val disk = DiskIn.ar( spec.numChannels, buf.id )
               val disk    = DiskIn.ar( spec.numChannels, buf.id, loop = 1 )
               val rls     = pRelease.kr
//               val pDur = spec.numFrames.toDouble / SampleRate.ir
               val pDur    = framesToSeconds( spec.numFrames )
               val pFreq   = 1.0 / pDur
               val lTim    = pDur - 1.0
               val lPhase  = lTim / pDur
               val lTrig   = Impulse.kr( pFreq, lPhase )
               val envGate = 1 - Latch.kr( rls, lTrig )
               val env     = EnvGen.kr( Env.asr( attack = 0.01, release = 1.0, shape = sinShape ), gate = envGate )
//             val env = Line.kr( 1, 1, dur = pDur )
               val me      = Proc.local
               Done.kr( env ).react {
                  threadAtomic( identifier + " : stop proc" ) { implicit tx => me.stop }
               }
               Mix.mono( disk ) * env
            }
         }
      }

      new Impl( file, fact, spec.numFrames )
   }

   private final class Impl( file: File, fact: ProcFactory, val length: Long ) extends Phrase with ExtractionImpl {
      import GraphemeUtil._

      def identifier = PhraseImpl.identifier

      override def toString = "Phrase.fromFile(" + file + ")"

      def printFormat : String = {
         "phrase( " + fileNameWithoutExtension( file ) + ", " + formatSeconds( framesToSeconds( length )) + " )"
      }

      private val featureRef = Ref( Option.empty[ File ])

      def player( implicit tx: Tx ) : Proc = fact.make

      def asStrugatzkiInput( implicit tx: Tx ) : FutureResult[ File ] = featureRef() match {
         case Some( res ) => futureOf( res )
         case None =>
            extract( file, None, false ).mapSuccess { res =>
               atomic( identifier + " : cache feature extraction" ) { implicit tx =>
                  featureRef.set( Some( res ))
               }
               res
            }
      }

      def reader( implicit tx: Tx ) : FrameReader.Factory = FrameReader.Factory( file )
   }
}