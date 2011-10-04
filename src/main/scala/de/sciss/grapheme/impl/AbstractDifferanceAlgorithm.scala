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
         val futTgt  = ovs.map( ov => databaseQuery.find( p, ov )( tx1 )) : FutureResult[ IIdxSeq[ DifferanceDatabaseQuery.Match ]]
         val pNow    = FutureResult.now( p )( tx1 )
         val futPNew= futTgt flatMap { targets =>
            (ovs zip targets).foldRight( pNow ) { case ((ov, target), futP1) =>
               atomic( "DifferanceAlgorithm : overwriter.perform" )( tx2 =>
                  futP1.flatMap( p1 => overwriter.perform( p1, ov, target )( tx2 )))
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
