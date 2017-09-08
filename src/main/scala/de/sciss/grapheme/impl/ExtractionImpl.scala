/*
 *  ExtractionImpl.scala
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
import de.sciss.strugatzki.FeatureExtraction

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

trait ExtractionImpl[S <: Sys[S]] {

  import GraphemeUtil._

  protected def identifier: String

  /**
    * Starts an extraction process for a given audio file input. The process
    * is started after the transaction commits, and the method returns
    * the future result of the meta file thus generated.
    */
  protected def extract(audioInput: File, dir: Option[File], keep: Boolean)(implicit tx: S#Tx): Future[File] = {
    val res = Promise[File]()
    tx.afterCommit {
      import FeatureExtraction._
      val set           = Config()
      set.audioInput    = audioInput
      set.featureOutput = createTempFile(".aif", dir, keep)
      val meta = createTempFile("_feat.xml", dir, keep)
      set.metaOutput = Some(meta)
      val process = apply(set)
      process.addListener {
        case Result(_, Failure(Aborted())) =>
          val e = new RuntimeException(s"$identifier process aborted")
          res.failure(e)

        case Result(_, Failure(e)) =>
          res.failure(e)

        case Result(_, Success(_)) =>
          res.success(meta)

        case Progress(_, _) =>
      }
      process.start()
    }
    res.future
  }
}