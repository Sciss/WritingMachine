/*
 *  DifferanceSpatImpl.scala
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
import de.sciss.synth
import de.sciss.synth.proc.Proc

import scala.collection.immutable.{IndexedSeq => Vec}

object DifferanceSpatImpl {
  def apply[S <: Sys[S]](collector: Proc[S])(implicit tx: S#Tx): DifferanceSpat[S] = {
    import synth._
    import ugen._

    val numCh = WritingMachine.masterNumChannels

    val dsl = DSL[S]
    import dsl._

    val diffs = Vec.tabulate(numCh) { ch =>
      val p = filter(s"out-${ch + 1}", numChannels = 1) { in =>
        val sig = if (ch == 0) {
          Seq(in, Silent.ar(numCh - 1))
        } else if (ch == numCh - 1) {
          Seq(Silent.ar(numCh - 1), in)
        } else {
          Seq(Silent.ar(ch), in, Silent.ar(numCh - 1 - ch))
        }
        Flatten(sig)
      }

      p ~> collector
      p
    }

    new DifferanceSpatImpl[S](diffs)
  }
}

class DifferanceSpatImpl[S <: Sys[S]] private(diffs: Vec[Proc[S]])
  extends AbstractDifferanceSpat[S] {

  def numChannels: Int = diffs.size

  def diffusion(chan: Int)(implicit tx: S#Tx): Proc[S] = diffs(chan)
}