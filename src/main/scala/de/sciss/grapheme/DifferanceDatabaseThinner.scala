/*
 *  DifferanceDatabaseThinner.scala
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

import de.sciss.grapheme.impl.{DifferanceDatabaseThinnerImpl => Impl}
import de.sciss.lucre.stm.Sys
import de.sciss.span.Span

import scala.collection.immutable.{IndexedSeq => Vec}
import scala.concurrent.Future

object DifferanceDatabaseThinner {
  def apply[S <: Sys[S]](database: Database[S]): DifferanceDatabaseThinner[S] = Impl[S](database)
}

trait DifferanceDatabaseThinner[S <: Sys[S]] {
  def remove(spans: Vec[Span])(implicit tx: S#Tx): Future[Unit]
}