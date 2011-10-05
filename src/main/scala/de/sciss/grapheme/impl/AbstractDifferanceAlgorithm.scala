/*
 *  AbstractDifferanceAlgorithm.scala
 *  (WritingMachine)
 *
 *  Copyright (c) 2011 Hanns Holger Rutz. All rights reserved.
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

import collection.breakOut
import collection.immutable.{IndexedSeq => IIdxSeq}

abstract class AbstractDifferanceAlgorithm extends DifferanceAlgorithm {
   def startPhrase: Phrase

   def spat: DifferanceSpat
   def overwriteSelector: DifferanceOverwriteSelector
   def overwriter: DifferanceOverwriter
   def databaseQuery: DifferanceDatabaseQuery
   def phraseTrace: PhraseTrace
   def thinner: DifferanceDatabaseThinner
   def filler: DifferanceDatabaseFiller

   private val phraseRef = Ref( startPhrase )

   def step( implicit tx: Tx ) : FutureResult[ Unit ] = step0( tx )

   private def step0( tx0: Tx ) : FutureResult[ Unit ] = {
      val p          = phraseRef()( tx0 )
      val futSpat    = spat.rotateAndProject( p )( tx0 )
      val futOvrSel  = overwriteSelector.selectParts( p )( tx0 )
      futOvrSel.flatMap( mapOverwrites( p, futSpat ) _ )
   }

   private def mapOverwrites( p: Phrase, fut0: FutureResult[ Unit ] )
                            ( instrs: IIdxSeq[ OverwriteInstruction ]) : FutureResult[ Unit ] = {
      atomic( "DifferanceAlgorithm : mapOverwrites" ) { tx1 =>
         val ovs     = instrs.sortBy( _.span.start )
//         val futTgt  = ovs.map( ov => databaseQuery.find( p, ov )( tx1 )) : FutureResult[ IIdxSeq[ DifferanceDatabaseQuery.Match ]]
         val tgtNow  = FutureResult.now( IIdxSeq.empty[ DifferanceDatabaseQuery.Match ])( tx1 )
         val futTgt  = ovs.foldLeft( tgtNow ) { case (futTgt1, ov) =>
            futTgt1.flatMap { coll =>
               atomic( "DifferanceAlgorithm : databaseQuery" ) { tx2 =>
                  val futOne = databaseQuery.find( p, ov )( tx2 )
                  futOne.map { m =>
                     coll :+ m
                  }
               }
            }
         }
         val pNow    = FutureResult.now( p )( tx1 )
         val futPNew= futTgt flatMap { targets =>
            (ovs zip targets).foldRight( pNow ) { case ((ov, target), futP1) =>
               futP1.flatMap { p1 =>
                  atomic( "DifferanceAlgorithm : overwriter.perform" ) { tx2 =>
                     overwriter.perform( p1, ov, target )( tx2 )
                  }
               }
            }
         }
         val futThin = futTgt flatMap { targets =>
            atomic( "DifferanceAlgorithm : thinner.remove" )( tx3 =>
               thinner.remove( targets.map( _.span )( breakOut ))( tx3 ))
         }
         val futUnit1 = futPNew flatMap { pNew =>
            atomic( "DifferanceAlgorithm : update" ) { tx4 =>
               phraseRef.set( pNew )( tx4 )
               phraseTrace.add( pNew )( tx4 )
               FutureResult.now( () )( tx4 )
            }
         }
         val futUnit2 = futThin flatMap { _ =>
            atomic( "DifferanceAlgorithm : filler.perform" )( tx5 => filler.perform( tx5 ))}
         FutureResult.unitSeq( futUnit1, futUnit2, fut0 )
      }
   }
}
