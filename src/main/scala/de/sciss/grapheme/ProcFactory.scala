/*
 *  ProcFactory.scala
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

import de.sciss.lucre.stm.Sys
import de.sciss.synth.proc.Proc

object ProcFactory {
  def apply[S <: Sys[S]](proc: Proc[S])(implicit tx: S#Tx): ProcFactory[S] = new ProcFactory[S] {
    private[this] val procH = tx.newHandle(proc)

    def make()(implicit tx: S#Tx): Proc[S] = procH()
  }
}
trait ProcFactory[S <: Sys[S]] {
  def make()(implicit tx: S#Tx): Proc[S]
}
