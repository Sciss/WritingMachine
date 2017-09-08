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

import collection.immutable.{IndexedSeq => Vec}
import de.sciss.synth
import synth.proc.{DSL, Proc}

object DifferanceSpatImpl {
  def apply(collector: Proc)(implicit tx: Tx): DifferanceSpat = {
    import synth._
    import ugen._
    import DSL._

    val numCh = WritingMachine.masterNumChannels
    //      val fact       = filter( "spat" ) {
    //         val pChan   = pControl( "chan", ParamSpec( 0, numCh - 1, LinWarp, 1 ), 0 )
    //         graph { in: In =>
    //            val ch   = pChan.kr
    ////            ch := pos / 2 * numChannels
    //            val pos  = 2 * ch / numCh
    //            PanAz.ar( numCh, in, pos )
    //         }
    //      }

    //      val fact       = filter( "spat" ) {
    //         val pChan   = pControl( "chan", ParamSpec( 0, numCh - 1, LinWarp, 1 ), 0 )
    //         graph { in: In =>
    //            val ch   = pChan.kr
    //            Seq.tabulate( numCh )( i => in * (1 - (ch - i).min( 1 ))) : GE
    //         }
    //      }

    val dummyF = gen("$dummy")(graph {
      Silent.ar
    })
    val diffs = Vec.tabulate(numCh) { ch =>
      val fact = filter(/* "spat-" + */ (ch + 1).toString) {
        graph { in: In =>
          val sig = if (ch == 0) {
            Seq(in, Silent.ar(numCh - 1))
          } else if (ch == numCh - 1) {
            Seq(Silent.ar(numCh - 1), in)
          } else {
            Seq(Silent.ar(ch), in, Silent.ar(numCh - 1 - ch))
          }
          Flatten(sig)
        }
      }
      val p = fact.make
      //         p.control( "chan" ).v = ch
      val dummy = dummyF.make
      dummy ~> p ~> collector
      p.play
      dummy.dispose
      p
    }
    new DifferanceSpatImpl(diffs)
  }
}

class DifferanceSpatImpl private(diffs: Vec[Proc])
  extends AbstractDifferanceSpat {

  def numChannels: Int = diffs.size

  def diffusion(chan: Int)(implicit tx: Tx): Proc = diffs(chan)
}