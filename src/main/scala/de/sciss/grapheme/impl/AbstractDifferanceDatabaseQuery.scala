/*
 *  AbstractDifferanceDatabaseQuery.scala
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
import de.sciss.span.Span

import scala.concurrent.Future

object AbstractDifferanceDatabaseQuery {
  private val verbose = false
}

abstract class AbstractDifferanceDatabaseQuery[S <: Sys[S]] extends DifferanceDatabaseQuery[S] {

  import GraphemeUtil._
  import DifferanceDatabaseQuery._
  import AbstractDifferanceDatabaseQuery._

  /**
    * Approximate duration of the cross-correlation in seconds.
    */
  def matchDurationMotion: Motion

  /**
    * Spectral weight in strugatzki (0 = temporal breaks, 1 = spectral breaks,
    * 0.5 = mix of both).
    */
  def spectralMotion: Motion

  /**
    * Maximum allowed deviation from target duration (factor-offset,
    * e.g. 0 = no deviation allowed, 0.5 = 50% deviation allowed).
    */
  def stretchDeviationMotion: Motion

  /**
    * Maximum deviation from number one match. (e.g. 0 = always take
    * best match, 1 = 50% best match and 50% second-best match,
    * 9 = 10% probability of choice among the 10 best matches)
    */
  def rankMotion: Motion

  private def matchLength()(implicit tx: Tx): Long = {
    val matchDur = matchDurationMotion.step
    secondsToFrames(/* fact * */ matchDur)
  }

  def minPhraseDur: Double

  def findMatch(rank: Int, phrase: Phrase[S], punchIn: Span, punchOut: Span,
                minPunch: Long, maxPunch: Long, weight: Double)(implicit tx: S#Tx): Future[Match[S]]

  def find(phrase: Phrase[S], overwrite: OverwriteInstruction)(implicit tx: S#Tx): Future[Match[S]] = {
    val spect       = spectralMotion.step

    val stretchDev  = stretchDeviationMotion.step
    val minConstr   = secondsToFrames(0.1)
    val min0        = (overwrite.newLength / (1 + stretchDev)).toLong
    val max0        = (overwrite.newLength * (1 + stretchDev)).toLong
    val pDur        = framesToSeconds(phrase.length)
    val min1        = if (pDur > minPhraseDur) min(phrase.length / 2, min0) else min0
    val max1        = if (pDur > minPhraseDur) min(phrase.length / 2, max0) else max0
    val minPunch    = max(minConstr, min1)
    val maxPunch    = max(minConstr, max1)

    val inLen       = matchLength()
    val outLen      = matchLength()
    val inner       = (minPunch + 1) / 2 + maxPunch / 2
    val (piStop, poStart) = if (inner >= overwrite.span.length) {
      (overwrite.span.start + (inLen + 1) / 2,
        overwrite.span.stop - outLen / 2)
    } else { // move punchIn to left and punchOut to right, but keep touching point at same ratio
      val r = ((minPunch + 1) / 2).toDouble / inner
      val mid = (r * overwrite.span.length).toLong + overwrite.span.start
      (mid, mid)
    }
    val piStart   = max(0L, piStop - inLen)
    val poStop    = min(phrase.length, poStart + outLen)

    val punchIn   = Span(piStart, piStop)
    val punchOut  = Span(poStart, poStop)

    if (verbose) {
      val punchInS  = formatSpan(punchIn)
      val punchOutS = formatSpan(punchOut)
      val minPunchS = formatSeconds(framesToSeconds(minPunch))
      val maxPunchS = formatSeconds(framesToSeconds(minPunch))
      println(s"---punch in $punchInS / out $punchOutS / minPunch = $minPunchS / maxPunch = $maxPunchS")
    }

    val rank = random(max(0, rankMotion.step.toInt) + 1)

    findMatch(rank, phrase, punchIn, punchOut, minPunch, maxPunch, spect)
  }
}