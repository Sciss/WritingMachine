/*
 *  DifferanceDatabaseFillerImpl.scala
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

import de.sciss.lucre.stm.Sys

object DifferanceDatabaseFillerImpl {
  def apply[S <: Sys[S]](db: Database[S], tv: Television[S])(implicit tx: S#Tx): DifferanceDatabaseFiller[S] = {
    new DifferanceDatabaseFillerImpl[S](db, tv)
  }
}

class DifferanceDatabaseFillerImpl[S <: Sys[S]] private(val database: Database[S], val television: Television[S])
  extends AbstractDifferanceDatabaseFiller[S] {

  val durationMotion: Motion = Motion.constant(180.0)
  val maxCaptureDur: Double = 45.0
}