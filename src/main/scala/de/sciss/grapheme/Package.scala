/*
 *  package grapheme
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

package de.sciss

import de.sciss.lucre.stm.TxnLike
import de.sciss.synth.proc

import scala.language.implicitConversions

package object grapheme {
  type Tx = TxnLike // proc.ProcTxn
  //   type Future[ A ] = scala.actors.Future[ A ]
  type Ref[A] = proc.Ref[A]

  object Ref {
    def apply[A: ClassManifest](init: A): Ref[A] = proc.Ref(init)

    def empty[A: ClassManifest]: Ref[A] = proc.Ref.make[A]
  }

  def atomic[A](info: => String)(fun: Tx => A): A = {
    proc.ProcTxn.atomic { tx =>
      GraphemeUtil.logTx(info)(tx)
      fun(tx)
    }
  }

  //   implicit def wrapFutureResultSeq[ A ]( fs: Vec[ FutureResult[ A ]]) : FutureResult[ Vec[ A ]] =
  //      FutureResult.enrich( fs )

  implicit def enrichProc(p: proc.Proc): RichProc = RichProc(p)
}