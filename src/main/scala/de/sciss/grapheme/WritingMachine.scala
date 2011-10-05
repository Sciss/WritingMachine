/*
 *  WritingMachine.scala
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

import de.sciss.nuages.NuagesLauncher
import de.sciss.synth.proc.ProcTxn

object WritingMachine {
   val logPanel            = false
   val masterChannelOffset = 0
   val masterNumChannels   = 9
   val tvChannelOffset     = 0

   val name          = "WritingMachine"
   val version       = 0.10
   val copyright     = "(C)opyright 2011 Hanns Holger Rutz"
   val isSnapshot    = true

   def versionString = {
      val s = (version + 0.001).toString.substring( 0, 4 )
      if( isSnapshot ) s + "-SNAPSHOT" else s
   }

   def printInfo() {
      println( "\n" + name + " v" + versionString + "\n" + copyright + ". All rights reserved.\n" )
   }

   def main( args: Array[ String ]) {
      args.toSeq match {
//         case Seq( "--fut1" ) => Futures.test1()
         case _ => launch()
      }
   }

   def launch() {
      val cfg = NuagesLauncher.SettingsBuilder()
      cfg.beforeShutdown   = quit _
      cfg.doneAction       = booted _
      val o                = cfg.serverOptions
      val masterChans      = (masterChannelOffset until (masterChannelOffset + masterNumChannels)).toIndexedSeq
      o.outputBusChannels  = masterChans.max + 1
      o.inputBusChannels   = tvChannelOffset + 1
      cfg.masterChannels   = Some( masterChans )
      cfg.collector        = true
      val c                = cfg.controlSettings
      c.log                = logPanel
      val i                = c.replSettings
      i.imports          :+= "de.sciss.grapheme._"
      i.text               =
"""val init = Init.instance
val phrase = Phrase.fromFile( new java.io.File( "/Users/hhrutz/Desktop/SP_demo/tapes/Affoldra_RoomLp.aif" ))
init.spat.rotateAndProject( phrase )
"""
      NuagesLauncher( cfg )
   }

   def quit() {
      println( "Bye bye..." )
   }

   def booted( r: NuagesLauncher.Ready ) {
      ProcTxn.atomic { implicit tx =>
         Init( r )
      }
   }
}