/*
 *  DifferanceAlgorithm.scala
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

import de.sciss.grapheme.impl.{DifferanceAlgorithmImpl => Impl}
import de.sciss.lucre.stm
import de.sciss.lucre.stm.Sys

import scala.concurrent.Future

object DifferanceAlgorithm {
  def apply[S <: Sys[S]](
                        thinner           : DifferanceDatabaseThinner   [S],
                        filler            : DifferanceDatabaseFiller    [S],
                        phraseTrace       : PhraseTrace                 [S],
                        databaseQuery     : DifferanceDatabaseQuery     [S],
                        overwriter        : DifferanceOverwriter        [S],
                        overwriteSelector : DifferanceOverwriteSelector [S],
                        startPhrase       : Phrase                      [S]
                        )(implicit cursor: stm.Cursor[S]): DifferanceAlgorithm[S] = {
    Impl(/* spat, */ thinner, filler, phraseTrace, databaseQuery, overwriter, overwriteSelector, startPhrase)
  }
}

trait DifferanceAlgorithm[S <: Sys[S]] {
  def step(implicit tx: S#Tx): Future[Phrase[S]]
}