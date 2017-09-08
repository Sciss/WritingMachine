/*
 *  DifferanceOverwriteSelector.scala
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

import de.sciss.grapheme.impl.{DifferanceOverwriteSelectorImpl => Impl}
import de.sciss.lucre.stm
import de.sciss.lucre.stm.Sys

import scala.collection.immutable.{IndexedSeq => Vec}
import scala.concurrent.Future

object DifferanceOverwriteSelector {
  def apply[S <: Sys[S]]()(implicit cursor: stm.Cursor[S]): DifferanceOverwriteSelector[S] = Impl()
}

trait DifferanceOverwriteSelector[S <: Sys[S]] {
  def selectParts(phrase: Phrase[S])(implicit tx: S#Tx): Future[Vec[OverwriteInstruction]]
}