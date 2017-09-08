/*
 *  DifferanceAlgorithmImpl.scala
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