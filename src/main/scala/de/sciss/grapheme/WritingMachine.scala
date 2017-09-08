/*
 *  WritingMachine.scala
 *  (WritingMachine)
 *
 *  Copyright (c) 2011-2017 Hanns Holger Rutz. All rights reserved.
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

import de.sciss.synth.proc.ProcTxn
import de.sciss.strugatzki.Strugatzki
import collection.immutable.{IndexedSeq => Vec}
import de.sciss.osc.TCP
import swing.Swing
import de.sciss.nuages.{NuagesFrame, NuagesLauncher}
import java.awt.event.ActionEvent
import javax.swing.{JButton, AbstractAction}
import java.io.File

object WritingMachine {
  private val settings = new WritingMachineSettings

  import settings._

  val autoStart           : Boolean     = getBool("auto-start", default = true)
  val logPanel            : Boolean     = false
  val masterChannelOffset : Int         = getInt("master-channel-offset", 0) // 2
  val soloChannelOffset   : Option[Int] = Option.empty // Some( 10 ) // Some( 0 )
  val masterNumChannels   : Int         = getInt("master-num-channels", 6) // 9
  val tvChannelOffset     : Int         = getInt("tv-channel-offset", 2) // 0
  val tvNumChannels       : Int         = getInt("tv-num-channels", 2)
  val tvPhaseFlip         : Boolean     = getBool("tv-phase-flip", default = true)
  val tvBoostDB           : Double      = getDouble("tv-boost", 0.0) // decibels
  val tvUseTestFile       : Boolean     = getBool("tv-use-test-file", default = true) // false
  val phraseBoostDB       : Double      = getDouble("phrase-boost", 3.0) // decibels
  //   val strugatzkiDatabase  = new File( "/Users/hhrutz/Documents/devel/LeereNull/feature/" )
  val inDevice            : String      = "MOTU 828mk2"
  val outDevice           : String      = inDevice
  val baseDir             : String      = "/Applications/WritingMachine"
  val databaseDir         : File        = new File(new File(baseDir, "audio_work"), "database")
  val testDir             : File        = new File(new File(baseDir, "audio_work"), "test")
  val restartUponTimeout  : Boolean     = getBool("restart-upon-timeout", default = true)
  val restartAfterTime    : Double      = getDouble("restart-after-time", 15 * 60.0)
  val restartUponException: Boolean     = getBool("restart-upon-exception", default = true)
  val supercolliderPath   : String      = getString("supercollider-path", "")
  val initialPhraseFill   : Double      = getDouble("initial-phrase-fill", 20.0)

  val name = "WritingMachine"
  val version = 0.10
  val copyright = "(C)opyright 2011 Hanns Holger Rutz"
  val isSnapshot = true

  def versionString: String = {
    val s = (version + 0.001).toString.substring(0, 4)
    if (isSnapshot) s + "-SNAPSHOT" else s
  }

  def printInfo(): Unit =
    println("\n" + name + " v" + versionString + "\n" + copyright + ". All rights reserved.\n")

  def main(args: Array[String]): Unit = {
    args.toSeq match {
      //         case Seq( "--fut1" ) => Futures.test1()

      case _ => launch()
    }
  }

  def launch(): Unit = {
    //      sys.props += "actors.enableForkJoin" -> "false"

    val cfg = NuagesLauncher.SettingsBuilder()
    cfg.beforeShutdown = quit _
    cfg.doneAction = booted _
    val o = cfg.serverOptions
    if (supercolliderPath != "") o.programPath = supercolliderPath
    o.host = "127.0.0.1"
    o.pickPort()
    //      o.transport          = TCP
    o.deviceNames = Some((inDevice, outDevice))
    val masterChans = (masterChannelOffset until (masterChannelOffset + masterNumChannels)).toIndexedSeq
    val soloChans = soloChannelOffset.map(off => Vec(off, off + 1)).getOrElse(Vec.empty)
    o.outputBusChannels = {
      val mm = masterChans.max + 1
      if (soloChans.isEmpty) mm else {
        math.max(mm, soloChans.max + 1)
      }
    }
    o.inputBusChannels = tvChannelOffset + tvNumChannels
    cfg.masterChannels = Some(masterChans)
    cfg.soloChannels = if (soloChans.nonEmpty) Some(soloChans) else None
    cfg.collector = true
    val c = cfg.controlSettings
    c.log = logPanel
    c.numInputChannels = tvNumChannels
    c.numOutputChannels = masterNumChannels
    c.clockAction = { (state, fun) =>
      if (state) {
        atomic(name + " : start") { implicit tx => Init.instance.start() }
      } else {
        atomic(name + " : stop") { implicit tx => Init.instance.stop() }
      }
      fun()
    }

    val i = c.replSettings
    i.imports :+= "de.sciss.grapheme._"
    i.text =
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
    NuagesLauncher(cfg)
  }

  private def quit(): Unit =
    println("Bye bye...")

  def restart(): Unit = {
    println("==== Restarting ====")
    sys.exit(1)
  }

  def shutDownComputer(): Unit = {
    val pb = new ProcessBuilder("/bin/sh", new File("shutdown.sh").getAbsolutePath)
    pb.start()
  }

  def booted(r: NuagesLauncher.Ready): Unit = {
    Strugatzki.tmpDir = GraphemeUtil.tmpDir
    if (restartUponTimeout) ProcTxn.timeoutFun = () => {
      restart()
    }
    if (restartAfterTime > 0) {
      val t = new java.util.Timer()
      val millis = (restartAfterTime * 1000).toLong
      t.schedule(new java.util.TimerTask {
        def run() {
          restart()
        }
      }, millis)
    }

    //      FeatureExtraction.verbose = true

    Swing.onEDT(initGUI(r.frame))

    ProcTxn.atomic { implicit tx =>
      Init(r)
    }
  }

  def initGUI(f: NuagesFrame): Unit = {
    f.bottom.add(new JButton(new AbstractAction("SHUTDOWN") {
      def actionPerformed(e: ActionEvent) {
        shutDownComputer()
      }
    }))
    f.bottom.add(new JButton(new AbstractAction("RESTART") {
      def actionPerformed(e: ActionEvent) {
        restart()
      }
    }))
  }
}