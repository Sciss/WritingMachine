/*
 *  DifferanceOverwriteSelectorImpl.scala
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

import de.sciss.lucre.stm
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.stm.TxnLike.peer
import de.sciss.processor.Processor.{Aborted, Progress, Result}
import de.sciss.span.Span
import de.sciss.strugatzki.{FeatureExtraction, FeatureSegmentation}
import de.sciss.synth.io.AudioFile

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

object DifferanceOverwriteSelectorImpl {
  val verbose = false

  def apply[S <: Sys[S]]()(implicit cursor: stm.Cursor[S]): DifferanceOverwriteSelector[S] =
    new DifferanceOverwriteSelectorImpl[S]()

  private val minDur        =   3.0
  private val minStabDur    =  10.0
  private val maxDur        = 150.0
  private val stableDurProb =   3.0 / 100

  private val identifier    = "overwrite-selector-impl"
}

final class DifferanceOverwriteSelectorImpl[S <: Sys[S]]()(implicit val cursor: stm.Cursor[S])
  extends AbstractDifferanceOverwriteSelector[S] {

  import DifferanceOverwriteSelectorImpl._
  import GraphemeUtil._

  val fragmentDurationMotion  : Motion = Motion.linexp(Motion.walk(0, 1, 0.1), 0, 1, 0.5, 4.0) //  Motion.exprand( 0.5, 4.0 )
  val fragmentDeviationMotion : Motion = Motion.constant(0.5)
  val positionMotion          : Motion = Motion.linexp(Motion.walk(0, 1, 0.1), 0, 1, 0.25, 4.0)
  val frequencyMotion         : Motion = Motion.walk(2, 16, 1.0) // Motion.constant( 8 )
  val spectralMotion          : Motion = Motion.walk(0.25, 0.75, 0.1) // Motion.linrand( 0.25, 0.75 )

  /**
    * Length of correlation in seconds
    */
  val correlationMotion       : Motion = Motion.linexp(Motion.walk(0, 1, 0.1), 0, 1, 0.2, 2.0) // Motion.exprand( 0.250, 1.250 )

  //   val numBreaks                 = 200

  private val stretchStable   : Motion = Motion.linexp(Motion.walk(0, 1, 0.1), 0, 1, 1.0 / 1.1, 1.1)
  private val stretchGrow     : Motion = Motion.walk(1.2, 2.0, 0.2)
  private val stretchShrink   : Motion = Motion.walk(0.6, 0.95, 0.2)

  private val stretchMotion = Ref(stretchStable)

  def stretchMotion(phrase: Phrase[S])(implicit tx: S#Tx): Motion = {
    val pDur = framesToSeconds(phrase.length)
    if (pDur <= minDur) {
      stretchMotion.set(stretchGrow)
      if (verbose) println(s"---pDur = ${formatSeconds(pDur)} -> grow")
      stretchGrow
    } else if (pDur >= maxDur) {
      stretchMotion.set(stretchShrink)
      if (verbose) println(s"---pDur = ${formatSeconds(pDur)} -> shrink")
      stretchShrink
    } else if (pDur > minStabDur && random < stableDurProb) {
      stretchMotion.set(stretchStable)
      if (verbose) println(s"---pDur = ${formatSeconds(pDur)} -> stable")
      stretchStable
    } else {
      stretchMotion()
    }
  }

  /**
    * Note that currently Strugatzki doesn't allow a 'punchOut' for segmentation.
    * Our algorithm thus proceeds as follows:
    *
    * - determine the correlation length from motion
    * - search span start is max( 0, center - maxLen/2 - corrLen )
    * - search span stop is min( fileLen, span start + maxLen + 2*corrLen )
    * - find one break
    * - return a span around that break (is possible covering center) for
    * a random length between minLen and maxLen
    *
    * Previous, more complicated idea:
    *
    * - determine the correlation length from motion
    * - search span start is max( 0, center - maxLen/2 - corrLen )
    * - search span stop is min( fileLen, span start + maxLen + 2*corrLen )
    * - find an arbitrary number of breaks (e.g. 200)
    * - set min spacing to span length / num breaks
    * - with each break from the result:
    *   - go through the possible punch-out candidates (e.g. those that
    * are spaced so that minLen and maxLen are met)
    *   - from the candidates keep track of the best choice (highest dissimilarity)
    */
  def bestPart(phrase: Phrase[S], center: Long, minLen: Long, maxLen: Long, weight: Double)
              (implicit tx: S#Tx): Future[Span] = {
    val strugFut = phrase.asStrugatzkiInput
    strugFut.flatMap { metaInput =>
      bestPartWith(metaInput, center, minLen, maxLen, weight)
    }
  }

  private def bestPartWith(metaInput: File, center: Long, minLen: Long, maxLen: Long,
                           weight: Double): Future[Span] = {
    val res             = Promise[Span]()
    val extr            = FeatureExtraction.Config.fromXMLFile(metaInput)
    val spec            = AudioFile.readSpec(extr.audioInput)
    val set             = FeatureSegmentation.Config()
    set.databaseFolder  = databaseDir
    set.metaInput       = metaInput
    val corrLen         = cursor.atomic(s"$identifier : correlation motion") { tx1 =>
      secondsToFrames(correlationMotion.step(tx1))
    }
    val maxLenH         = maxLen / 2
    val start           = max(0L, center - maxLenH - corrLen)
    val stop            = min(spec.numFrames, start + maxLen + corrLen + corrLen)
    set.span            = Span(start, stop)
    set.corrLen         = corrLen
    set.temporalWeight  = weight.toFloat
    set.normalize       = true
    set.numBreaks       = 2
    set.minSpacing      = 0
    val setb = set.build
    val segm = FeatureSegmentation(setb)
    segm.addListener {
      case Result(_, Failure(Aborted())) =>
        val e = new RuntimeException(s"$identifier process aborted")
        res.failure(e)

      case Result(_, Failure(e)) =>
        res.failure(e)

      case Result(_, Success(coll)) =>
        if (coll.isEmpty) {
          if (math.random > 0.1) {
            val e = new RuntimeException(s"$identifier process yielded no break")
            res.failure(e)
          } else {
            val len = math.min(spec.numFrames, (math.random * (maxLen - minLen) + minLen).toLong)
            val start = (math.random * (spec.numFrames - len)).toLong
            res.success(Span(start, start + len))
          }
        } else {
          val b = if (coll(0).sim.isNaN) coll(1) else coll(0)
          val len = /* cursor.atomic(s"$identifier : found ${formatSeconds(framesToSeconds(b.pos))}") { tx2 => */
            (random * (maxLen - minLen) + minLen).toLong
//          }

          val s = if (b.pos <= center) {
            val stop0 = max(center, b.pos + len / 2)
            val start = max(0L, stop0 - len)
            val stop = min(spec.numFrames, start + len)
            Span(start, stop)
          } else {
            val start0 = min(center, b.pos - len / 2)
            val stop = min(spec.numFrames, start0 + len)
            val start = max(0L, stop - len)
            Span(start, stop)
          }
          res.success(s)
        }

      case Progress(_, _) =>
    }
    segm.start()
    res.future
  }
}