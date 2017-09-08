/*
 *  PhraseTrace.scala
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

import de.sciss.lucre.stm.Sys
import impl.{PhraseTraceImpl => Impl}

import collection.immutable.{IndexedSeq => Vec}

object PhraseTrace {
  def apply[S <: Sys[S]](): PhraseTrace[S] = Impl()
}

trait PhraseTrace[S <: Sys[S]] {
  def add(phrase: Phrase[S])(implicit tx: S#Tx): Unit

  def series(n: Int)(implicit tx: S#Tx): Vec[Phrase[S]]
}