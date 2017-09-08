/*
 *  DifferanceDatabaseFiller.scala
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

import de.sciss.grapheme.impl.{DifferanceDatabaseFillerImpl => Impl}
import de.sciss.lucre.stm.Sys

import scala.concurrent.Future

object DifferanceDatabaseFiller {
  def apply[S <: Sys[S]](db: Database[S], tv: Television[S])(implicit tx: S#Tx): DifferanceDatabaseFiller[S] =
    Impl[S](db, tv)
}

trait DifferanceDatabaseFiller[S <: Sys[S]] {
  def database: Database[S]

  def perform(implicit tx: S#Tx): Future[Unit]
}