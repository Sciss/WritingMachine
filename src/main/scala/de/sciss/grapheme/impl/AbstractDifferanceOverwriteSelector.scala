/*
 *  AbstractDifferanceOverwriteSelector.scala
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

import de.sciss.lucre.stm
import de.sciss.lucre.stm.Sys
import de.sciss.span.Span
import de.sciss.synth

import collection.immutable.{IndexedSeq => Vec}
import scala.concurrent.Future
import scala.util.{Failure, Success}

object AbstractDifferanceOverwriteSelector {
  private val verbose = true
}

abstract class AbstractDifferanceOverwriteSelector[S <: Sys[S]]
  extends DifferanceOverwriteSelector[S] {

  import GraphemeUtil._
  import AbstractDifferanceOverwriteSelector._

  protected def cursor: stm.Cursor[S]

  private val identifier = "a-overwrite-selector"

  /**
    * Temporal stretch factor (1 = preserve duration, <1 = shorten, >1 = elongate).
    */
  def stretchMotion(phrase: Phrase[S])(implicit tx: S#Tx): Motion

  /**
    * Spectral weight in strugatzki (0 = temporal breaks, 1 = spectral breaks,
    * 0.5 = mix of both).
    */
  def spectralMotion: Motion

  /**
    * Approximate duration of fragments in seconds.
    */
  def fragmentDurationMotion: Motion

  /**
    * Maximum allowed deviation from fragment duration (factor-offset,
    * e.g. 0 = no deviation allowed, 0.5 = 50% deviation allowed).
    */
  def fragmentDeviationMotion: Motion

  /**
    * Probability skew of fragment positions (power factor, thus
    * 1 = same probability at beginning and ending, >1 = more towards
    * beginning, <1 = more towards ending).
    */
  def positionMotion: Motion

  /**
    * Amount of parts selected per step.
    */
  def frequencyMotion: Motion

  def bestPart(phrase: Phrase[S], center: Long, minLen: Long, maxLen: Long, weight: Double)
              (implicit tx: S#Tx): Future[Span]

  def selectParts(phrase: Phrase[S])(implicit tx: S#Tx): Future[Vec[OverwriteInstruction]] = {
    val num = frequencyMotion.step.toInt
    val ovrNow = futureOf(Vec.empty[OverwriteInstruction])
    selectPartsWith(ovrNow, phrase, num)
  }

  private def selectPartsWith(ovrNow: Future[Vec[OverwriteInstruction]], phrase: Phrase[S],
                              num: Int): Future[Vec[OverwriteInstruction]] = {
    (0 until num).foldLeft(ovrNow) { case (futPred, i) =>
      futPred.flatMap { coll =>
        val (stretch, futSpan) = cursor.atomic(s"$identifier : select parts") { tx1 =>
          import synth._

          val stre    = stretchMotion(phrase)(tx1).step(tx1)
          val spect   = spectralMotion.step(tx1)
          val fragDur = fragmentDurationMotion.step(tx1)
          val fragDev = fragmentDeviationMotion.step(tx1)
          val minFrag = secondsToFrames(fragDur / (1 + fragDev))
          val maxFrag = secondsToFrames(fragDur * (1 + fragDev))

          val posPow  = positionMotion.step(tx1)
          val pos     = random.pow(posPow).linlin(0, 1, 0, phrase.length).toLong

          (stre, bestPart(phrase, pos, minFrag, maxFrag, spect)(tx1))
        }

        futSpan.transform {
          case Failure(_) =>
            if (verbose) println(s"---no best part result for $i / $num")
            Success(coll) // :+ None
          case Success(span) =>
            val newLen = (span.length * stretch).toLong
            if (verbose) {
              val fromS = formatSeconds(framesToSeconds(span.length))
              val toS   = formatSeconds(framesToSeconds(newLen))
              println(s"---stretch from $fromS to $toS (${formatPercent(stretch)})")
            }
            val ins = OverwriteInstruction(span, newLen)
            Success(coll :+ ins) // Some( ins )
        }
      }
    }
  }
}