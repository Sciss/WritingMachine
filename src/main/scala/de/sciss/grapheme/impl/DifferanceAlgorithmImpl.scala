package de.sciss.grapheme
package impl

object DifferanceAlgorithmImpl {
   def apply( spat: DifferanceSpat,
              thinner: DifferanceDatabaseThinner,
              filler: DifferanceDatabaseFiller,
              phraseTrace: PhraseTrace,
              databaseQuery: DifferanceDatabaseQuery,
              overwriter: DifferanceOverwriter,
              overwriteSelector: DifferanceOverwriteSelector,
              startPhrase: Phrase ) : DifferanceAlgorithm = {
      new DifferanceAlgorithmImpl( spat, thinner, filler, phraseTrace, databaseQuery, overwriter, overwriteSelector,
                                   startPhrase )
   }
}

class DifferanceAlgorithmImpl private ( val spat: DifferanceSpat,
                                        val thinner: DifferanceDatabaseThinner,
                                        val filler: DifferanceDatabaseFiller,
                                        val phraseTrace: PhraseTrace,
                                        val databaseQuery: DifferanceDatabaseQuery,
                                        val overwriter: DifferanceOverwriter,
                                        val overwriteSelector: DifferanceOverwriteSelector,
                                        val startPhrase: Phrase )
extends AbstractDifferanceAlgorithm {

}