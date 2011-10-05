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
   def fromFile( file: File )( implicit tx: Tx ) : Phrase = {
      import synth._
      import proc._
      import ugen._
      import DSL._

      val path       = file.getAbsolutePath
      val spec       = audioFileSpec( path )
      val factName   = "phrase-file-" + file.getName // XXX hrmpfff
      val fact       = ProcDemiurg.factories.find( _.name == factName ).getOrElse {
         gen( factName ) {
            graph {
               val buf  = bufCue( path )
               val disk = DiskIn.ar( spec.numChannels, buf.id )
               // Done.kr( disk )
               val me   = Proc.local
               Done.kr( Line.kr( dur = spec.numFrames.toDouble / SampleRate.ir )).react {
                  atomic( "PhraseImpl : stop proc" ) { implicit tx => me.stop }
               }
               Mix.mono( disk )
            }
         }
      }

      new Impl( fact, spec.numFrames )
   }

   private class Impl( fact: ProcFactory, val length: Long ) extends Phrase {
      def player( implicit tx: Tx ) : Proc = fact.make

      def asStrugatzkiInput( implicit tx: Tx ) : FutureResult[ File ] = sys.error( "TODO" )
   }
}