package de.sciss.grapheme
package impl

import collection.immutable.{IndexedSeq => IIdxSeq}
import de.sciss.synth
import synth.proc.{DSL, Proc}

object DifferanceSpatImpl {
   def apply( collector: Proc )( implicit tx: Tx ) : DifferanceSpat = {
      import synth._
      import ugen._
      import DSL._

      val numCh      = WritingMachine.masterNumChannels
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

      val dummyF     = gen( "$dummy" )( graph { Silent.ar })
      val diffs      = IIdxSeq.tabulate( numCh ) { ch =>
         val fact       = filter( "spat-" + (ch + 1) ) {
            graph { in: In =>
               val sig = if( ch == 0 ) {
                  Seq( in, Silent.ar( numCh - 1 ))
               } else if( ch == numCh - 1 ) {
                  Seq( Silent.ar( numCh - 1 ), in )
               } else {
                  Seq( Silent.ar( ch ), in, Silent.ar( numCh - 1 - ch ))
               }
               Flatten( sig )
            }
         }
         val p       = fact.make
//         p.control( "chan" ).v = ch
         val dummy   = dummyF.make
         dummy ~> p ~> collector
         p.play
         dummy.dispose
         p
      }
      new DifferanceSpatImpl( diffs )
   }
}
class DifferanceSpatImpl private ( diffs: IIdxSeq[ Proc ])
extends AbstractDifferanceSpat {
   def numChannels = diffs.size
   def diffusion( chan: Int )( implicit tx: Tx ) : Proc = diffs( chan )
}