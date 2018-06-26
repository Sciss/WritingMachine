/*
 *  AbstractDifferanceDatabaseFiller.scala
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
        performWithFile(f, off = secondsToFrames(television.latency), inc = inc)
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