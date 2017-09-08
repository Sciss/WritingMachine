/*
 *  DifferanceSpat.scala
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