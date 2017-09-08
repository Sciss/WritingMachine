/*
 *  DifferanceSpat.scala
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
import impl.DifferanceSpatImpl
import de.sciss.synth.proc.Proc

import scala.concurrent.Future

object DifferanceSpat {
  def apply[S <: Sys[S]](collector: Proc[S])(implicit tx: S#Tx): DifferanceSpat[S] =
    DifferanceSpatImpl(collector)
}

trait DifferanceSpat[S <: Sys[S]] {
  /**
    * Projects the given phase onto the next channel. The returned future is
    * resolved, after releasePhrase has been called, so do not forget to call
    * releasePhrase for every channel!
    */
  def rotateAndProject(phrase: Phrase[S])(implicit tx: S#Tx): Future[Unit]
}