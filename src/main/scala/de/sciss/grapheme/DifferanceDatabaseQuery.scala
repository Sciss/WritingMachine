/*
 *  DifferanceDatabaseQuery.scala
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
import de.sciss.span.Span
import impl.{DifferanceDatabaseQueryImpl => Impl}

import scala.concurrent.Future

object DifferanceDatabaseQuery {

  final case class Match[S <: Sys[S]](database: Database[S], span: Span, boostIn: Float, boostOut: Float) {

    import GraphemeUtil._

    def printFormat: String = {
      val s = formatSpan(span)
      s"match($s)"
    }
  }

  def apply[S <: Sys[S]](db: Database[S]): DifferanceDatabaseQuery[S] = Impl(db)
}

trait DifferanceDatabaseQuery[S <: Sys[S]] {

  import DifferanceDatabaseQuery._

  def find(phrase: Phrase[S], overwrite: OverwriteInstruction)(implicit tx: S#Tx): Future[Match[S]]
}