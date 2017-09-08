/*
 *  AbstractDifferanceSpat.scala
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
package impl

import de.sciss.lucre.stm.Sys
import de.sciss.lucre.stm.TxnLike.peer
import de.sciss.synth.proc.Proc

import scala.concurrent.Future

abstract class AbstractDifferanceSpat[S <: Sys[S]] extends DifferanceSpat[S] {
  private val identifier = "a-differance-spat"

  def numChannels: Int

  def diffusion(chan: Int)(implicit tx: S#Tx): Proc[S]

  private val chanRef = Ref(-1)
  private val procRef = Ref(Option.empty[Proc[S]])

  def rotateAndProject(phrase: Phrase[S])(implicit tx: S#Tx): Future[Unit] = {
    val chan = (chanRef() + 1) % numChannels
    chanRef.set(chan)
    val pProc = phrase.player
    val pDiff = diffusion(chan)
    ???
//    pProc ~> pDiff
//    pProc.play
//    procRef.swap(Some(pProc)).foreach { oldProc =>
//      oldProc.control("release").v = 1
//    }
//    pProc.futureStopped.map { e => // even in case of failure
//      atomic(identifier + " : dispose phrase process") { implicit tx =>
//        pProc.remove
//      }
//      e // pass on failure!
//    }
  }
}