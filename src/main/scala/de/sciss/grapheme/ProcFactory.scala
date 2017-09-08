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
