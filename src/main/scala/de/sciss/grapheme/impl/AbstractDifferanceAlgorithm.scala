/*
 *  AbstractDifferanceAlgorithm.scala
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

import de.sciss.grapheme.DifferanceDatabaseQuery.Match
import de.sciss.lucre.stm
import de.sciss.lucre.stm.Sys

import scala.collection.breakOut
import scala.collection.immutable.{IndexedSeq => Vec}
import scala.concurrent.Future
import scala.util.Success

abstract class AbstractDifferanceAlgorithm[S <: Sys[S]] extends DifferanceAlgorithm[S] {

  import GraphemeUtil._
  
  protected def cursor: stm.Cursor[S]

  private val identifier = "a-differance"

  def startPhrase: Phrase[S]

  def overwriteSelector : DifferanceOverwriteSelector [S]
  def overwriter        : DifferanceOverwriter        [S]
  def databaseQuery     : DifferanceDatabaseQuery     [S]
  def phraseTrace       : PhraseTrace                 [S]
  def thinner           : DifferanceDatabaseThinner   [S]
  def filler            : DifferanceDatabaseFiller    [S]

  private val phraseRef = Ref(startPhrase)

  def step(implicit tx: S#Tx): Future[Phrase[S]] = step0(tx)

  private def step0(tx0: S#Tx): Future[Phrase[S]] = {
    val p = phraseRef()(tx0.peer)
    val futOvrSel = overwriteSelector.selectParts(p)(tx0)
    futOvrSel.flatMap(mapOverwrites(p))
  }

  private def mapOverwrites(p: Phrase[S] /*, fut0: Future[ Unit ]*/)
                           (instructions: Vec[OverwriteInstruction]): Future[Phrase[S]] = {

    val futFill = cursor.atomic(s"$identifier : filler.perform")(tx5 => filler.perform(tx5))

    val ovs: Seq[OverwriteInstruction] = instructions.sortBy(_.span.start)
    val tgtNow: Future[Vec[Option[Match[S]]]] = futFill.map(_ => Vec.empty[Option[Match[S]]])
    val futTgt = ovs.foldLeft(tgtNow) { case (futTgt1, ov) =>
      futTgt1.flatMap { coll =>
        cursor.atomic(s"$identifier : database query ${ov.printFormat}") { implicit tx =>
          val futOne = databaseQuery.find(p, ov)
          //               futOne.mapSuccess { m =>
          //                  coll :+ m
          //               }
          futOne.transform { res =>
            val resOpt = res.toOption
            val coll1 = coll :+ resOpt // .toOption
            Success(coll1)
          }
        }
      }
    }
    val pNow = futureOf(p)
    val futPNew = futTgt.flatMap { targets0 =>
      val targets = targets0.collect({ case Some(tgt) => tgt })
      (ovs zip targets).foldRight(pNow) { case ((ov, target), futP1) =>
        futP1.flatMap { p1 =>
          cursor.atomic(s"$identifier : overwriter perform ${target.printFormat}") { tx2 =>
            overwriter.perform(p1, ov, target)(tx2)
          }
        }
      }
    }
    val futThin = futTgt.flatMap { targets0 =>
      val targets = targets0.collect({ case Some(tgt) => tgt })
      cursor.atomic(s"$identifier : thinner remove ${targets.size} regions")(tx3 =>
        thinner.remove(targets.map(_.span)(breakOut))(tx3))
    }
    val futResult = futPNew.map { pNew =>
      cursor.atomic(s"$identifier complete cycle with ${pNew.printFormat}") { tx4 =>
        phraseRef.set(pNew)(tx4.peer)
        phraseTrace.add(pNew)(tx4)
        pNew
      }
    }

    futThin.flatMap(_ => futResult)
  }
}
