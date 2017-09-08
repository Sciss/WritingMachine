/*
 *  PhraseTraceImpl.scala
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

import scala.collection.immutable.{IndexedSeq => Vec}

object PhraseTraceImpl {
  private val identifier = "phrase-trace-impl"

  def apply[S <: Sys[S]](): PhraseTrace[S] = new PhraseTraceImpl[S]()
}

final class PhraseTraceImpl[S <: Sys[S]] private() extends PhraseTrace[S] {

  import GraphemeUtil._
  import PhraseTraceImpl._

  def add(phrase: Phrase[S])(implicit tx: S#Tx): Unit =
    warnToDo(s"$identifier : add")

  def series(n: Int)(implicit tx: S#Tx): Vec[Phrase[S]] = {
    //      warnToDo( "PhraseTraceImpl : series" )
    //      Vec.empty
    sys.error("TODO")
  }
}