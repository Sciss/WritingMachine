/*
 *  PhraseTraceImpl.scala
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