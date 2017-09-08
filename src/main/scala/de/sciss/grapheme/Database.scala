/*
 *  Database.scala
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