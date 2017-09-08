/*
 *  PhraseImpl.scala
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
import de.sciss.synth.io.AudioFile
import de.sciss.synth.proc.Proc

import scala.concurrent.Future

object PhraseImpl {

  import GraphemeUtil._

  private val identifier = "phrase-impl"

  def fromFile[S <: Sys[S]](file: File)(implicit tx: S#Tx, cursor: stm.Cursor[S]): Phrase[S] = {

    val spec = AudioFile.readSpec(file) // audioFileSpec(path)
    require(spec.numChannels == 1) // we need this now for the overwriter implementation!

    val factName = s"file-${fileNameWithoutExtension(file)}" // XXX hrmpfff
    val fact = ???
//      ProcDemiurg.factories.find(_.name == factName).getOrElse {
//      gen(factName) {
//        val pRelease = pControl("release", ParamSpec(0, 1, LinearWarp, 1), 0)
//        val pAmp = pControl("amp", ParamSpec(0.5, 10, LinearWarp), WritingMachine.phraseBoostDB.dbamp)
//        graph {
//          val buf     = bufCue(path)
//          val disk    = DiskIn.ar(spec.numChannels, buf.id, loop = 1)
//          val rls     = pRelease.kr
//          val pDur    = math.max(0.5, framesToSeconds(spec.numFrames))
//          val pFreq   = 1.0 / pDur
//          val lTim    = pDur - 1.0
//          val lPhase  = lTim / pDur
//          val lTrig   = Impulse.kr(pFreq, lPhase)
//          val envGate = 1 - Latch.kr(rls, lTrig)
//          val env     = EnvGen.kr(Env.asr(attack = 0.01, release = 1.0, curve = Curve.sine), gate = envGate)
//          val me      = Proc.local
//          Done.kr(env).react {
//            threadAtomic(s"$identifier : stop proc") { implicit tx => me.stop }
//          }
//          Mix.mono(disk) * env * pAmp.kr
//        }
//      }
//    }

    new Impl[S](file, fact, spec.numFrames)
  }

  private final class Impl[S <: Sys[S]](file: File, fact: ProcFactory[S], val length: Long)
                                       (implicit cursor: stm.Cursor[S])
    extends Phrase[S] with ExtractionImpl[S] {

    import GraphemeUtil._

    def identifier: String = PhraseImpl.identifier

    override def toString = s"Phrase.fromFile($file)"

    def printFormat: String =
      s"phrase(${fileNameWithoutExtension(file)}, ${formatSeconds(framesToSeconds(length))})"

    private val featureRef = Ref(Option.empty[File])

    def player(implicit tx: S#Tx): Proc[S] = fact.make()

    def asStrugatzkiInput(implicit tx: S#Tx): Future[File] = featureRef() match {
      case Some(res) => futureOf(res)
      case None =>
        extract(file, None, keep = false).map { res =>
          cursor.atomic(s"$identifier : cache feature extraction") { implicit tx =>
            featureRef.set(Some(res))
          }
          res // Future.Success(res)
        }
    }

    def reader(implicit tx: S#Tx): FrameReader.Factory = FrameReader.Factory(file)
  }
}