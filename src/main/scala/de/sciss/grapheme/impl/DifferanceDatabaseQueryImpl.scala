/*
 *  DifferanceDatabaseQueryImpl.scala
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

import de.sciss.lucre.stm.Sys
import de.sciss.processor.Processor.{Aborted, Progress, Result}
import de.sciss.span.Span
import de.sciss.strugatzki.FeatureCorrelation
import de.sciss.synth

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

object DifferanceDatabaseQueryImpl {
  var verbose = false

  def apply[S <: Sys[S]](db: Database[S]): DifferanceDatabaseQuery[S] =
    new DifferanceDatabaseQueryImpl[S](db)
}

class DifferanceDatabaseQueryImpl[S <: Sys[S]] private(db: Database[S])
  extends AbstractDifferanceDatabaseQuery[S] {

  import DifferanceDatabaseQuery._
  import GraphemeUtil._

  private val identifier = "database-query-impl"

  val matchDurationMotion: Motion = Motion.coin(
    1.0 / 33,
    Motion.linexp(Motion.walk(0, 1, 0.1), 0, 1, 0.4, 4.0), // Motion.exprand( 0.4, 4.0 )
    Motion.exprand(4.0, 16.0)
  )
  //   val matchDeviationMotion   = Motion.linrand( 0.2, 0.5 )
  val spectralMotion        : Motion = Motion.linrand(0.25, 0.75)
  val stretchDeviationMotion: Motion = Motion.walk(0.111, 0.333, 0.05) // Motion.linrand( 0.2, 0.5 )
  val rankMotion            : Motion = Motion.linrand(0, 11)

  val maxBoostMotion        : Motion = Motion.constant(30) // 18
  val minSpacingMotion      : Motion = Motion.constant(0.0) // 0.5

  val minPhraseDur = 10.0

  def findMatch(rank: Int, phrase: Phrase[S], punchIn: Span, punchOut: Span,
                minPunch: Long, maxPunch: Long, weight: Double)(implicit tx: S#Tx): Future[Match[S]] = {

    import synth._

    val maxBoost  = maxBoostMotion.step.dbamp.toFloat
    val minSpc    = secondsToFrames(minSpacingMotion.step)
    val dirFut    = db.asStrugatziDatabase
    val metaFut   = phrase.asStrugatzkiInput

    dirFut.flatMap{ dir =>
      metaFut.flatMap { metaInput =>
        findMatchIn(dir, metaInput, maxBoost, minSpc, rank, phrase, punchIn, punchOut,
          minPunch, maxPunch, weight)
      }
    }
  }

  private def findMatchIn(dir: File, metaInput: File, maxBoost: Float, minSpacing: Long, rank: Int,
                          phrase: Phrase[S],
                          punchIn: Span, punchOut: Span,
                          minPunch: Long, maxPunch: Long, weight: Double): Future[Match[S]] = {

    import FeatureCorrelation.{Match => _, _} // otherwise Match shadows DifferanceDatabaseQuery.Match

    val res = Promise[Match[S]]()
    val set = Config()
    set.databaseFolder  = dir
    set.normalize       = true
    set.maxBoost        = maxBoost
    set.metaInput       = metaInput
    set.minSpacing      = minSpacing
    set.numMatches      = max(2, rank + 1)
    set.numPerFile      = max(2, rank + 1)

    set.punchIn = Punch(Span(punchIn.start, punchIn.stop), weight.toFloat)
    set.punchOut = Some(Punch(Span(punchOut.start, punchOut.stop), weight.toFloat))
    set.minPunch = minPunch
    set.maxPunch = maxPunch

    val setb = set.build

    if (verbose) println("----CORRELATION----")
    if (verbose) println(setb)

    val process = apply(setb)
    process.addListener {
      case Result(_, Failure(Aborted())) =>
        val e = new RuntimeException(identifier + " process aborted")
        res.failure(e)

      case Result(_, Failure(e)) =>
        res.failure(e)

      case Result(_, Success(coll)) =>
        val idx0 = min(coll.size - 1, rank)
        val idx = if (idx0 == 0 && coll(idx0).sim.isNaN) idx0 + 1 else idx0
        if (idx < 0 || idx >= coll.size) {
          val e = new RuntimeException(identifier + " process yielded no matches")
          res.failure(e)
        } else {
          val m = coll(idx)
          res.success(Match(db, Span(m.punch.start, m.punch.stop), m.boostIn, m.boostOut))
        }

      case Progress(_, _) =>
    }
    process.start()
    res.future
  }

//  private def failureMatch = {
//    atomic(identifier + " : failure match") { tx1 =>
//      Match(db, Span(0L, min(db.length(tx1), secondsToFrames(1.0))), 1f, 1f)
//    }
//  }
}