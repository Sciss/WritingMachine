/*
 *  WritingMachine.scala
 *  (WritingMachine)
 *
 *  Copyright (c) 2011-2017 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.grapheme

import de.sciss.file._
import de.sciss.lucre.stm
import de.sciss.lucre.synth.{InMemory, Sys => SSys}
import de.sciss.nuages.{Nuages, NuagesFrame, NuagesView}
import de.sciss.strugatzki.Strugatzki
import de.sciss.submin.Submin
import de.sciss.synth.Server
import de.sciss.synth.proc.{AuralSystem, Folder}

import scala.swing.{Button, Swing}

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
  val audioDevice         : String      = "Wr_t_ngM_ch_n_"
  val baseDir             : File        = file("/") / "data" / "projects" / "Wr_t_ngM_ch_n_"
  val databaseDir         : File        = baseDir / "audio_work" / "database"
  val testDir             : File        = baseDir / "audio_work" / "test"
  val restartAfterTime    : Double      = getDouble("restart-after-time", 15 * 60.0)
  val restartUponException: Boolean     = getBool("restart-upon-exception", default = true)
  val initialPhraseFill   : Double      = getDouble("initial-phrase-fill", 20.0)

  def main(args: Array[String]): Unit = {
    Submin.install(true)
    type S = InMemory
    implicit val system: S = InMemory()
    launch[S]()
  }

  def launch[S <: SSys[S]]()(implicit cursor: stm.Cursor[S]): Unit = {
//    val cfg = NuagesLauncher.SettingsBuilder()
//    cfg.beforeShutdown = quit _
//    cfg.doneAction = booted _
//    val o = cfg.serverOptions
//    if (supercolliderPath != "") o.programPath = supercolliderPath
//    o.host = "127.0.0.1"
//    o.pickPort()
//    //      o.transport          = TCP
//    o.deviceNames = Some((inDevice, outDevice))
//    val masterChans = (masterChannelOffset until (masterChannelOffset + masterNumChannels)).toIndexedSeq
//    val soloChans = soloChannelOffset.map(off => Vec(off, off + 1)).getOrElse(Vec.empty)
//    o.outputBusChannels = {
//      val mm = masterChans.max + 1
//      if (soloChans.isEmpty) mm else {
//        math.max(mm, soloChans.max + 1)
//      }
//    }
//    o.inputBusChannels = tvChannelOffset + tvNumChannels
//    cfg.masterChannels = Some(masterChans)
//    cfg.soloChannels = if (soloChans.nonEmpty) Some(soloChans) else None
//    cfg.collector = true
//    val c = cfg.controlSettings
//    c.log = logPanel
//    c.numInputChannels = tvNumChannels
//    c.numOutputChannels = masterNumChannels
//    c.clockAction = { (state, fun) =>
//      if (state) {
//        atomic(s"$name : start") { implicit tx => Init.instance.start() }
//      } else {
//        atomic(s"$name : stop") { implicit tx => Init.instance.stop() }
//      }
//      fun()
//    }
//
////    val i = c.replSettings
////    i.imports :+= "de.sciss.grapheme._"
////    i.text =
////      """val init = Init.instance
////        |init.start()
////        |
////        |// val phrase = Phrase.fromFile( new java.io.File( "/Users/hhrutz/Desktop/SP_demo/tapes/Affoldra_RoomLp.aif" ))
////        |// init.spat.rotateAndProject( phrase )
////        |actors.Actor.actor {
////        |   val futStep = atomic( "algo step" )( tx => init.differance.step( tx ))
////        |   println( "==== Awaiting Diff Alg Step ====" )
////        |   futStep()
////        |   println( "==== Diff Alg Step Done ====" )
////        |}
////        |""".stripMargin
//    NuagesLauncher(cfg)

    val r = cursor.step { implicit tx =>
      val f     = Folder[S]
      val n     = Nuages[S](Nuages.Surface.Folder(f))
      val nCfg  = Nuages.Config()
      implicit val aural: AuralSystem = AuralSystem()
      import de.sciss.synth.proc.WorkspaceHandle.Implicits._
      val _view = NuagesView[S](n, nCfg)
      /* val frame = */ NuagesFrame(_view, undecorated = false /* true */)
      val aCfg  = Server.Config()
      aural.start(aCfg)
      _view.panel.transport.play()
      aural.whenStarted(_ => booted[S](_view))
      _view
    }
    Swing.onEDT(initGUI(r))
  }

//  private def quit(): Unit =
//    println("Bye bye...")

  def restart(): Unit = {
    println("==== Restarting ====")
    sys.exit(1)
  }

  def shutDownComputer(): Unit = {
    val pb = new ProcessBuilder("/bin/sh", new File("shutdown.sh").getAbsolutePath)
    pb.start()
  }

  def booted[S <: SSys[S]](r: NuagesView[S])(implicit cursor: stm.Cursor[S]): Unit = {
    Strugatzki.tmpDir = GraphemeUtil.tmpDir

    if (restartAfterTime > 0) {
      val t = new java.util.Timer()
      val millis = (restartAfterTime * 1000).toLong
      t.schedule(new java.util.TimerTask {
        def run(): Unit = restart()
      }, millis)
    }

    cursor.atomic("init") { implicit tx =>
      Init(r).start()
    }
  }

  def initGUI[S <: SSys[S]](view: NuagesView[S]): Unit = {
    view.addSouthComponent(Button("SHUTDOWN") {
      shutDownComputer()
    })
    view.addSouthComponent(Button("RESTART") {
      restart()
    })
  }
}