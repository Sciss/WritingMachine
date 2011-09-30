package de.sciss.grapheme
package impl

import de.sciss.synth.proc.Ref

abstract class DifferanceAlgorithmImpl extends DifferanceAlgorithm {
   def startPhrase: Phrase
   def spat: DifferanceSpat
   def overwriteSelector: DifferanceOverwriteSelector
   def overwriter: DifferanceOverwriter
   def databaseQuery: DifferanceDatabaseQuery
   def phraseTrace: PhraseTrace
   def thinner: DifferanceDatabaseThinner
   def filler: DifferanceDatabaseFiller

   private val phraseRef = Ref( startPhrase )

   def step( implicit tx: Tx ) {
      val p       = phraseRef()
      spat.rotateAndProject( p )
      val ovs     = overwriteSelector.selectParts( p ).sortBy( _.span.start )
      val targets = ovs.map( ov => databaseQuery.find( p, ov ))
      val pNew    = (ovs zip targets).foldRight( p ) { case ((ov, target), p1) =>
         overwriter.perform( p1, ov, target )
      }
      phraseTrace.add( pNew )
      val targets1 = Span.merge( targets )
      targets1.sortBy( _.start ).reverse.foreach( thinner.remove( _ ))
      // filler...
      phraseRef.set( pNew )
   }
}
