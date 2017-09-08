/*
 *  DifferanceSpatImpl.scala
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

//    val dummyF = gen("$dummy")(graph {
//      Silent.ar
//    })
    val diffs = Vec.tabulate(numCh) { ch =>
//      val fact = filter(/* "spat-" + */ (ch + 1).toString) {
//        graph { in: In =>
//          val sig = if (ch == 0) {
//            Seq(in, Silent.ar(numCh - 1))
//          } else if (ch == numCh - 1) {
//            Seq(Silent.ar(numCh - 1), in)
//          } else {
//            Seq(Silent.ar(ch), in, Silent.ar(numCh - 1 - ch))
//          }
//          Flatten(sig)
//        }
//      }
//      val p = fact.make
//      //         p.control( "chan" ).v = ch
//      val dummy = dummyF.make
//      dummy ~> p ~> collector
//      p.play
//      dummy.dispose
//      p
      ???
    }

    new DifferanceSpatImpl[S](diffs)
  }
}

class DifferanceSpatImpl[S <: Sys[S]] private(diffs: Vec[Proc[S]])
  extends AbstractDifferanceSpat[S] {

  def numChannels: Int = diffs.size

  def diffusion(chan: Int)(implicit tx: S#Tx): Proc[S] = diffs(chan)
}