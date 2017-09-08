/*
 *  DifferanceOverwriter.scala
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

import de.sciss.grapheme.DifferanceDatabaseQuery.Match
import de.sciss.grapheme.impl.{DifferanceOverwriterImpl => Impl}
import de.sciss.lucre.stm
import de.sciss.lucre.stm.Sys

import scala.concurrent.Future

object DifferanceOverwriter {
  def apply[S <: Sys[S]]()(implicit cursor: stm.Cursor[S]): DifferanceOverwriter[S] = Impl()
}

trait DifferanceOverwriter[S <: Sys[S]] {
  def perform(phrase: Phrase[S], source: OverwriteInstruction, target: Match[S])
             (implicit tx: S#Tx): Future[Phrase[S]]
}