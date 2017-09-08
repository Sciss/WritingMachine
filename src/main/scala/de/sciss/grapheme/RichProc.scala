/*
 *  RichProc.scala
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

import de.sciss.lucre.stm.Sys
import de.sciss.synth.proc.Proc

import scala.concurrent.Future

object RichProc {
  def apply[S <: Sys[S]](proc: Proc[S]): RichProc[S] = new Impl(proc)

  private final class Impl[S <: Sys[S]](proc: Proc[S]) extends RichProc[S] {
    def futureStopped(implicit tx: S#Tx): Future[Unit] = {
//      require(proc.isPlaying)
//      val fut = Future.event[Unit]()
//      lazy val l: Proc.Listener = new Proc.Listener {
//        def updated(u: Proc.Update): Unit = {
//          val state = u.state
//          //println( "JO " + state )
//          if (state.valid && !state.playing) {
//            atomic("proc : stopped listener") { implicit tx =>
//              proc.removeListener(l)
//              fut.succeed(())
//            }
//          }
//        }
//      }
//      proc.addListener(l)
//      fut
      ???
    }

    def remove(implicit tx: S#Tx): Unit = {
      ???
//      val state = proc.state
//      require(!(state.playing || state.fading), state)
//      proc.anatomy match {
//        case ProcFilter => disposeFilter
//        case _ => disposeGenDiff
//      }
    }

//    private def disposeFilter(implicit tx: Tx): Unit = {
//      val in      = proc.audioInput ("in" )
//      val out     = proc.audioOutput("out")
//      val inEs    = in  .edges.toSeq
//      val outEs   = out .edges.toSeq
//      val outEsF  = outEs.filterNot(_.targetVertex.name.startsWith("$")) // XXX tricky shit to determine the meters
//      if (inEs.size > 1 && outEsF.size > 1) {
//        println("WARNING : Filter is connected to several inputs and outputs! (" + proc.name + " : inputs = " +
//          inEs.map(_.sourceVertex) + " ; outputs = " + outEsF.map(_.targetVertex) + ")")
//      }
//      //         if( verbose && outes.nonEmpty ) println( "" + new java.util.Date() + " " + out + " ~/> " + outes.map( _.in ))
//      outEs.foreach(out ~/> _.in)
//      inEs.foreach(inE => {
//        val out = inE.out
//        //            if( verbose ) println( "" + new java.util.Date() + " " + out + " ~> " + outesf.map( _.in ))
//        outEsF.foreach(out ~> _.in)
//      })
//      // XXX tricky: this needs to be last, so that
//      // the pred out's bus isn't set to physical out
//      // (which is currently not undone by AudioBusImpl)
//      //         if( verbose && ines.nonEmpty ) println( "" + new java.util.Date() + " " + ines.map( _.out ) + " ~/> " + in )
//      inEs.foreach(_.out ~/> in)
//      proc.dispose
//    }
//
//    private def disposeGenDiff(implicit tx: Tx): Unit = {
//      val inEs  = proc.audioInputs  .flatMap(_.edges).toSeq // XXX
//      val outEs = proc.audioOutputs .flatMap(_.edges).toSeq // XXX
//      outEs .foreach(outE => outE .out ~/> outE .in)
//      inEs  .foreach(inE  => inE  .out ~/> inE  .in)
//      proc.dispose
//    }

  }
}

sealed trait RichProc[S <: Sys[S]] {
  /**
    * Returns a future which is resolved when the process is stopped.
    * Requires that the process is currently playing.
    */
  def futureStopped(implicit tx: S#Tx): Future[Unit]

  /**
    * Gently removes the process. Requires that the process
    * is currently not playing.
    */
  def remove(implicit tx: S#Tx): Unit
}