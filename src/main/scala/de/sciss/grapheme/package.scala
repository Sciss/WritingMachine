/*
 *  package.scala
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

package de.sciss

import de.sciss.lucre.stm
import de.sciss.lucre.stm.{Sys, TxnLike}
import de.sciss.synth.proc.Proc

import scala.concurrent.ExecutionContext
import scala.language.implicitConversions

package object grapheme {
  type Tx = TxnLike

  type Ref[A] = scala.concurrent.stm.Ref[A]
  val  Ref    = scala.concurrent.stm.Ref

  implicit val executionContext: ExecutionContext = ExecutionContext.global

//  def atomic[A](info: => String)(fun: S#Tx => A)(implicit cursor: stm.Cursor[S]): A = {
//    cursor.step { tx =>
//      GraphemeUtil.logTx(info)(tx)
//      fun(tx)
//    }
//  }

  implicit final class RichCursor[S <: Sys[S]](private val cursor: stm.Cursor[S]) extends AnyVal {
    def atomic[A](info: => String)(fun: S#Tx => A): A = {
      cursor.step { tx =>
        GraphemeUtil.logTx(info)(tx)
        fun(tx)
      }
    }
  }

  implicit def enrichProc[S <: Sys[S]](p: Proc[S]): RichProc[S] = RichProc(p)
}