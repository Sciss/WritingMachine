///*
// *  Future.scala
// *  (WritingMachine)
// *
// *  Copyright (c) 2011-2017 Hanns Holger Rutz. All rights reserved.
// *
// *  This software is free software; you can redistribute it and/or
// *  modify it under the terms of the GNU General Public License
// *  as published by the Free Software Foundation; either
// *  version 2, june 1991 of the License, or (at your option) any later version.
// *
// *  This software is distributed in the hope that it will be useful,
// *  but WITHOUT ANY WARRANTY; without even the implied warranty of
// *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// *  General Public License for more details.
// *
// *  You should have received a copy of the GNU General Public
// *  License (gpl.txt) along with this software; if not, write to the Free Software
// *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
// *
// *
// *  For further information, please contact Hanns Holger Rutz at
// *  contact@sciss.de
// */
//
//package de.sciss.grapheme
//
//import scala.actors.sciss.FutureActor
//import scala.concurrent.Future
//import scala.concurrent.stm.Txn
//import scala.language.implicitConversions
//import scala.util.control.NonFatal
//
//trait Future[+A] {
//
//  import Future._
//
//  def isSet: Boolean
//
//  def apply(): Result[A]
//
//  def map[B](fun: Result[A] => Result[B]): Future[B]
//
//  def flatMap[B](fun: Result[A] => Future[B]): Future[B]
//
//  final def mapSuccess[B](fun: A => Result[B]): Future[B] =
//    map {
//      case Success(value) => fun(value)
//      case f@Failure(_) => f
//    }
//
//  final def flatMapSuccess[B](fun: A => Future[B]): Future[B] =
//    flatMap {
//      case Success(value) => fun(value)
//      case f@Failure(_) => now(f)
//    }
//
//  private[grapheme] def peer: Future[Result[A]]
//}
//
//object Future {
//
//  object Result {
//    implicit def succeed[A](value: A): Result[A] = Success(value)
//  }
//
//  sealed trait Result[+A] {
//    def toOption: Option[A]
//  }
//
//  final case class Failure(e: Throwable) extends Result[Nothing] {
//    def toOption: Option[Nothing] = None
//  }
//
//  final case class Success[A](value: A) extends Result[A] {
//    def toOption = Some(value)
//  }
//
//  def now[A](value: Result[A]): Future[A] = {
//    val ev = event[A]() // hmmmm... too much effort?
//    ev.set(value)
//    ev
//  }
//
//  def nowSucceed[A](value: A): Future[A] = now(Success(value))
//
//  def nowFail[A](e: Throwable): Future[A] = now(Failure(e))
//
//  trait Event[A] extends Future[A] {
//    def set(result: Result[A]): Unit
//
//    final def succeed(value: A): Unit =
//      set(Success(value))
//
//    final def fail(e: Throwable): Unit =
//      set(Failure(e))
//  }
//
//  def event[A](): Future.Event[A] = new Future.Event[A] with Basic[A] {
//
//    case class Set(value: Result[A]) // warning: don't make this final -- scalac bug
//
//    val c = FutureActor.newChannel[Result[A]]()
//    val peer: FutureActor[Result[A]] = new FutureActor[Result[A]]({ syncVar =>
//      peer.react {
//        case Set(value) => syncVar.set(value)
//      }
//    }, c)
//    peer.start()
//
//    def set(value: Result[A]) {
//      peer ! Set(value)
//    }
//  }
//
//  private def wrap[A](fut: Future[Result[A]]): Future[A] =
//    new Future[A] with Basic[A] {
//      def peer: Future[Result[A]] = fut
//    }
//
//  private sealed trait Basic[A] {
//    me: Future[A] =>
//
//    def map[B](fun: Result[A] => Result[B]): Future[B] = wrap(Futures.future {
//      try {
//        fun(me.peer.apply())
//      } catch {
//        case NonFatal(e) => Failure(e)
//      }
//    })
//
//    def flatMap[B](fun: Result[A] => Future[B]): Future[B] = wrap(Futures.future {
//      try {
//        fun(me.peer.apply()).peer.apply()
//      } catch {
//        case NonFatal(e) => Failure(e)
//      }
//    })
//
//    def isSet: Boolean = peer.value.isDefined // isSet
//
//    def apply(): Result[A] = {
//      require(Txn.findCurrent.isEmpty, "Must not call future-apply within an active transaction")
//      peer.apply()
//    }
//  }
//
//}