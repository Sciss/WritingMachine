/*
 *  Phrase.scala
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

import de.sciss.grapheme.impl.PhraseImpl
import de.sciss.lucre.stm
import de.sciss.lucre.stm.Sys
import de.sciss.synth.proc.Proc

import scala.concurrent.Future

object Phrase {
  def fromFile[S <: Sys[S]](file: File)(implicit tx: S#Tx, cursor: stm.Cursor[S]): Phrase[S] =
    PhraseImpl.fromFile[S](file)
}

trait Phrase[S <: Sys[S]] {
  def printFormat: String

  def length: Long

  def player(implicit tx: S#Tx): Proc[S]

  def asStrugatzkiInput(implicit tx: S#Tx): Future[File]

  def reader(implicit tx: S#Tx): FrameReader.Factory
}