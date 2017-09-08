/*
 *  Database.scala
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

import java.io.File

import de.sciss.grapheme.impl.{DatabaseImpl => Impl}
import de.sciss.lucre.stm
import de.sciss.lucre.stm.Sys

import scala.collection.immutable.{IndexedSeq => Vec}
import scala.concurrent.Future

object Database {
  def apply[S <: Sys[S]](dir: File)(implicit tx: S#Tx, cursor: stm.Cursor[S]): Database[S] = Impl[S](dir)
}

trait Database[S <: Sys[S]] {
  def length(implicit tx: S#Tx): Long

  implicit def cursor: stm.Cursor[S]

  def append(source: File, offset: Long, length: Long)(implicit tx: S#Tx): Future[Unit]

  def remove(instructions: Vec[RemovalInstruction])(implicit tx: S#Tx): Future[Unit]

  /**
    * Returns a directory carrying the strugatzki meta files of
    * the database.
    */
  def asStrugatziDatabase(implicit tx: S#Tx): Future[File]

  def reader(implicit tx: S#Tx): FrameReader.Factory
}