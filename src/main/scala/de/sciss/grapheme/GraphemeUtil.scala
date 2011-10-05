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

trait GraphemeUtil {
   protected final def random( implicit tx: Tx ) : Double = math.random // XXX
   protected final def sampleRate : Double = 44100.0

   protected final def random( top: Int )( implicit tx: Tx ) : Int = (random * top).toInt
   protected final def secondsToFrames( secs: Double ) : Long = (secs * sampleRate + 0.5).toLong
   protected final def max( i: Int, is: Int* ) : Int = is.foldLeft( i )( _ max _ )
   protected final def max( n: Long, ns: Long* ) : Long = ns.foldLeft( n )( _ max _ )
   protected final def max( d: Double, ds: Double* ) : Double = ds.foldLeft( d )( _ max _ )
   protected final def min( i: Int, is: Int* ) : Int = is.foldLeft( i )( _ min _ )
   protected final def min( n: Long, ns: Long* ) : Long = ns.foldLeft( n )( _ min _ )
   protected final def min( d: Double, ds: Double* ) : Double = ds.foldLeft( d )( _ min _ )

   protected final def openMonoWrite( f: File ) : AudioFile =
      AudioFile.openWrite( f, AudioFileSpec( AudioFileType.IRCAM, SampleFormat.Float, 1, sampleRate ))

   protected final def strugatzkiDatabase = WritingMachine.strugatzkiDatabase

   protected final def createTempFile( suffix: String ) : File = {
      val res = File.createTempFile( "wm", suffix )
      res.deleteOnExit()
      res
   }
}