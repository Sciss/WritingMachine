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
import java.io.File
import de.sciss.strugatzki.Strugatzki
import collection.immutable.{IndexedSeq => IIdxSeq}

object WritingMachine {
   val logPanel            = false
   val masterChannelOffset = 0 // 2
   val soloChannelOffset   = Some( 0 ) // Some( 0 )
   val masterNumChannels   = 9
   val tvChannelOffset     = 0
   val tvNumChannels       = 2
   val tvBoostDB           = 0   // decibels
   val tvUseTestFile       = false
//   val strugatzkiDatabase  = new File( "/Users/hhrutz/Documents/devel/LeereNull/feature/" )
   val inDevice            = "MOTU 828mk2"
   val outDevice           = inDevice
   val databaseDir         = new File( "audio_work", "database" )
   val testDir             = new File( "audio_work", "test" )

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
      o.deviceNames        = Some( (inDevice, outDevice) )
      val masterChans      = (masterChannelOffset until (masterChannelOffset + masterNumChannels)).toIndexedSeq
      val soloChans        = soloChannelOffset.map( off => IIdxSeq( off, off + 1 )).getOrElse( IIdxSeq.empty )
      o.outputBusChannels  = {
         val mm = masterChans.max + 1
         if( soloChans.isEmpty ) mm else {
            math.max( mm, soloChans.max + 1 )
         }
      }
      o.inputBusChannels   = tvChannelOffset + tvNumChannels
      cfg.masterChannels   = Some( masterChans )
      cfg.soloChannels     = if( soloChans.nonEmpty ) Some( soloChans ) else None
      cfg.collector        = true
      val c                = cfg.controlSettings
      c.log                = logPanel
      c.numInputChannels   = tvNumChannels
      c.numOutputChannels  = masterNumChannels

      val i                = c.replSettings
      i.imports          :+= "de.sciss.grapheme._"
      i.text               =
"""val init = Init.instance
init.start()

// val phrase = Phrase.fromFile( new java.io.File( "/Users/hhrutz/Desktop/SP_demo/tapes/Affoldra_RoomLp.aif" ))
// init.spat.rotateAndProject( phrase )
actors.Actor.actor {
   val futStep = atomic( "algo step" )( tx => init.differance.step( tx ))
   println( "==== Awaiting Diff Alg Step ====" )
   futStep()
   println( "==== Diff Alg Step Done ====" )
}
"""
      NuagesLauncher( cfg )
   }

   def quit() {
      println( "Bye bye..." )
   }

   def booted( r: NuagesLauncher.Ready ) {
      Strugatzki.tmpDir = GraphemeUtil.tmpDir
//      FeatureExtraction.verbose = true
      ProcTxn.atomic { implicit tx =>
         Init( r )
      }
   }
}