/*
 *  AbstractDifferanceDatabaseFiller.scala
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

import java.io.File

import de.sciss.lucre.stm
import de.sciss.lucre.stm.Sys

import scala.concurrent.Future

abstract class AbstractDifferanceDatabaseFiller[S <: Sys[S]] extends DifferanceDatabaseFiller[S] {

  import GraphemeUtil._

  protected final def cursor: stm.Cursor[S] = database.cursor

  private[this] val identifier = "a-database-filler"

  /**
    * Target database length in seconds.
    */
  def durationMotion: Motion

  def database: Database[S]

  def television: Television[S]

  def maxCaptureDur: Double

  final def perform(implicit tx: S#Tx): Future[Unit] = {
    val tgtLen  = secondsToFrames(durationMotion.step)
    val dbLen   = database.length
    val inc0    = tgtLen - dbLen
    val maxF    = secondsToFrames(maxCaptureDur)
    val inc     = min(inc0, maxF)

    logTx(s"$identifier : gathering ${formatSeconds(framesToSeconds(inc))}")

    if (inc > 0) {
      television.capture(inc).flatMap { f =>
        performWithFile(f, secondsToFrames(television.latency), inc)
      }
    } else {
      futureOf(())
    }
  }

  private def performWithFile(file: File, off: Long, inc: Long): Future[Unit] = {
    cursor.atomic(s"$identifier : appending ${formatSeconds(framesToSeconds(inc))}") { tx1 =>
      database.append(file, off, inc)(tx1)
    }
  }
}