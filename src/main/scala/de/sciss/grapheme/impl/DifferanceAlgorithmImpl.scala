/*
 *  DifferanceAlgorithmImpl.scala
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

import de.sciss.lucre.stm
import de.sciss.lucre.stm.Sys

object DifferanceAlgorithmImpl {
   def apply[S <: Sys[S]]( /* spat: DifferanceSpat, */
              thinner             : DifferanceDatabaseThinner   [S],
              filler              : DifferanceDatabaseFiller    [S],
              phraseTrace         : PhraseTrace                 [S],
              databaseQuery       : DifferanceDatabaseQuery     [S],
              overwriter          : DifferanceOverwriter        [S],
              overwriteSelector   : DifferanceOverwriteSelector [S],
              startPhrase         : Phrase                      [S]
                         )(implicit cursor: stm.Cursor[S]) : DifferanceAlgorithm[S] = {
      new DifferanceAlgorithmImpl( /* spat, */ thinner, filler, phraseTrace, databaseQuery, overwriter, overwriteSelector,
                                   startPhrase )
   }
}

class DifferanceAlgorithmImpl[S <: Sys[S]] private ( /* val spat: DifferanceSpat, */
                                        val thinner           : DifferanceDatabaseThinner   [S],
                                        val filler            : DifferanceDatabaseFiller    [S],
                                        val phraseTrace       : PhraseTrace                 [S],
                                        val databaseQuery     : DifferanceDatabaseQuery     [S],
                                        val overwriter        : DifferanceOverwriter        [S],
                                        val overwriteSelector : DifferanceOverwriteSelector [S],
                                        val startPhrase       : Phrase                      [S]
                                                   )(implicit val cursor: stm.Cursor[S])
extends AbstractDifferanceAlgorithm[S] {

}