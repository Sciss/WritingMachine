/*
 *  Television.scala
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

import impl.{FileTelevisionImpl => FileImpl, LiveTelevisionImpl => LiveImpl}
import java.io.File

import de.sciss.lucre.stm.Sys

import scala.concurrent.Future

object Television {
  def fromFile[S <: Sys[S]](f: File): Television[S] = FileImpl[S](f)

  def live[S <: Sys[S]](): Television[S] = LiveImpl()
}

trait Television[S <: Sys[S]] {
  def capture(length: Long)(implicit tx: S#Tx): Future[File]

  /**
    * Latency in capture result in seconds
    */
  def latency: Double
}