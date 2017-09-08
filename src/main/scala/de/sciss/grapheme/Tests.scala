///*
// *  Tests.scala
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
//import java.io.File
//
//import collection.immutable.{IndexedSeq => Vec}
//import de.sciss.strugatzki.{FeatureCorrelation, FeatureExtraction, Strugatzki}
//
//import scala.concurrent.Future
//
//object Tests {
//  def main(args: Array[String]): Unit = {
//    Strugatzki        .tmpDir   = GraphemeUtil.tmpDir
//    FeatureExtraction .verbose  = true
//    FeatureCorrelation.verbose  = true
//
//    args.headOption match {
//      case Some("--fut1") => future1()
//      case Some("--fut2") => future2()
//      case Some("--fut3") => future3()
//      case Some("--fut4") => future4()
//      case Some("--segm") => segm()
//      case Some("--fill") => segmAndFill()
//      case Some("--parts") => findParts()
//      case Some("--over") => overwrite()
//      case _ => sys.error("Illegal arguments : " + args)
//    }
//  }
//
//  def segm(): Unit =
//    segmAndThen { (_, _) => }
//
//  def segmAndFill(): Unit =
//    segmAndFillAndThen { (_, _, _) => }
//
//  def findParts(): Unit =
//    findPartsAndThen { (_, _, _) => }
//
//  def overwrite(): Unit = {
//    import GraphemeUtil._
//
//    findPartsAndThen { (phrase, ovs, ms) =>
//      val ovr = DifferanceOverwriter()
//      val zipped = ovs zip ms
//      // make sure we truncate or expand from the end, as that way the successive overwrites still make sense
//      val sorted = zipped.sortBy { case (ov, _) => -ov.span.start }
//      val pFut = sorted.foldLeft(futureOf(phrase)) { case (fut, (ov, m)) =>
//        fut.flatMap(p => atomic("Test perform overwrite")(tx1 => ovr.perform(p, ov, m)(tx1)))
//      }
//      println("Test ==== Waiting for Overwrites ====")
//      val newPhrase = pFut()
//      println("Test ==== Overwrite Result ====")
//      println(newPhrase)
//    }
//  }
//
//  def findPartsAndThen(fun: (Phrase, Vec[OverwriteInstruction], Vec[DifferanceDatabaseQuery.Match]) => Unit): Unit = {
//    import GraphemeUtil._
//
//    segmAndFillAndThen { (phrase, ovs, fill) =>
//      val query = DifferanceDatabaseQuery(fill.database)
//      val tgtNow = futureOf(Vec.empty[DifferanceDatabaseQuery.Match])
//      val futTgt: Future[Vec[DifferanceDatabaseQuery.Match]] = ovs.foldLeft(tgtNow) { case (futTgt1, ov) =>
//        futTgt1.flatMap { coll =>
//          atomic("Test databaseQuery") { tx2 =>
//            val futOne = query.find(phrase, ov)(tx2)
//            futOne.map { m =>
//              coll :+ m
//            }
//          }
//        }
//      }
//      println("Test ==== Waiting for Matches ====")
//      futTgt.foreach { ms =>
//        println("Test ==== Match Results ====")
//        ms.foreach(println)
//        fun(phrase, ovs, ms)
//      }
//    }
//  }
//
//  def segmAndFillAndThen(fun: (Phrase, Vec[OverwriteInstruction], DifferanceDatabaseFiller) => Unit): Unit = {
//    val fill = atomic("Test open db") { tx1 =>
//      val d = Database(new File("audio_work", "database"))(tx1)
//      val tv = Television.fromFile(new File(new File("audio_work", "test"), "aljazeera1.aif"))
//      DifferanceDatabaseFiller(d, tv)(tx1)
//    }
//    segmAndThen { (phrase, ovs) =>
//      val fillFut = atomic("Test fill db") { tx1 =>
//        fill.perform(tx1)
//      }
//      println("Test ==== Waiting for Filling ====")
//      fillFut.foreach { _ =>
//        println("Test ==== Filling done ====")
//        fun(phrase, ovs, fill)
//      }
//    }
//  }
//
//  def segmAndThen(fun: (Phrase, Vec[OverwriteInstruction]) => Unit): Unit = {
//    val sel = DifferanceOverwriteSelector()
//    val (phrase, spanFut) = atomic("Test phrase.fromFile") { implicit tx =>
//      val p = Phrase.fromFile(new File("audio_work/test/amazonas_m.aif"))
//      (p, sel.selectParts(p))
//    }
//    // make a real one... Actor.actor {} produces a daemon actor,
//    // which gets GC'ed and thus makes the VM prematurely quit
//    new actors.Actor {
//      start()
//
//      def act(): Unit = {
//        println("Test ==== Waiting for Overwrites ====")
//        val ovs = spanFut.apply().toOption.get
//        println("Test ==== Results ====")
//        ovs.foreach(println _)
//        fun(phrase, ovs)
//      }
//    }
//  }
//
//  /**
//    * Tests the `apply` method of `Future` when it requires waiting for the result.
//    */
//  def future1(): Unit = {
//    import actors.{TIMEOUT, Actor}
//    import Actor._
//
//    actor {
//      val ev = Future.event[Int]()
//      println("Test spawing actor 2")
//      actor {
//        reactWithin(3000L) {
//          case TIMEOUT => ev.succeed(33)
//        }
//      }
//      println("Test entering await")
//      val res = ev.apply()
//      println(s"Test result = $res")
//    }
//  }
//
//  /**
//    * Tests the `apply` method of `Future` when it is instantly available.
//    */
//  def future2(): Unit = {
//    import actors.{TIMEOUT, Actor}
//    import Actor._
//
//    actor {
//      val ev = Future.event[Int]()
//      println("Test spawing actor 2")
//      actor {
//        ev.succeed(33)
//      }
//      println("Test sleeping")
//      receiveWithin(3000L) {
//        case TIMEOUT =>
//          println("Test entering await")
//          val res = ev.apply()
//          println(s"Test result = $res")
//      }
//    }
//  }
//
//  /**
//    * Tests the `map` method of `Future`.
//    */
//  def future3(): Unit = {
//    import actors.{TIMEOUT, Actor}
//    import Actor._
//
//    actor {
//      val ev = Future.event[Int]()
//      val evP1 = ev.mapSuccess(_ + 1)
//      actor {
//        reactWithin(3000L) {
//          case TIMEOUT => ev.succeed(33)
//        }
//      }
//      println("Test entering await")
//      val res = evP1.apply()
//      println("Test result = " + res)
//    }
//  }
//
//  /**
//    * Tests the `flatMap` method of `Future`.
//    */
//  def future4(): Unit = {
//    import actors.{TIMEOUT, Actor}
//    import Actor._
//
//    actor {
//      val ev = Future.event[Int]()
//      val evm = ev.flatMapSuccess { i =>
//        val ev2 = Future.event[Int]()
//        actor {
//          reactWithin(3000L) {
//            case TIMEOUT =>
//              println("Test Setting ev2")
//              ev2.succeed(i * i)
//          }
//        }
//        ev2
//      }
//      actor {
//        reactWithin(3000L) {
//          case TIMEOUT =>
//            println("Test Setting ev")
//            ev.succeed(33)
//        }
//      }
//      println("Test entering await")
//      val res = evm.apply()
//      println(s"Test result = $res")
//    }
//  }
//}