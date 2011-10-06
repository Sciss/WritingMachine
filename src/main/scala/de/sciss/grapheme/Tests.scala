/*
 *  Tests.scala
 *  (WritingMachine)
 *
 *  Copyright (c) 2011 Hanns Holger Rutz. All rights reserved.
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

import java.io.File
import collection.immutable.{IndexedSeq => IIdxSeq}
import de.sciss.strugatzki.{Strugatzki, FeatureExtraction}

object Tests {
   def main( args: Array[ String ]) {
      Strugatzki.tmpDir = GraphemeUtil.tmpDir
      FeatureExtraction.verbose = true

      args.headOption match {
         case Some( "--fut1" )   => future1()
         case Some( "--fut2" )   => future2()
         case Some( "--fut3" )   => future3()
         case Some( "--fut4" )   => future4()
         case Some( "--segm" )   => segm()
         case Some( "--fill" )  => segmAndFill()
         case Some( "--parts" )  => findParts()
         case _ => sys.error( "Illegal arguments : " + args )
      }
   }

   def segm() {
      segmAndThen { (_, _) => }
   }

   def segmAndFill() {
      segmAndFillAndThen { (_, _, _) => }
   }

   def findParts() {
      import GraphemeUtil._

      segmAndFillAndThen { (phrase, ovs, fill) =>
         val query   = DifferanceDatabaseQuery( fill.database )
         val tgtNow  = futureOf( IIdxSeq.empty[ DifferanceDatabaseQuery.Match ])
         val futTgt  = ovs.foldLeft( tgtNow ) { case (futTgt1, ov) =>
            futTgt1.flatMap { coll =>
               atomic( "Test databaseQuery" ) { tx2 =>
                  val futOne = query.find( phrase, ov )( tx2 )
                  futOne.map { m =>
                     coll :+ m
                  }
               }
            }
         }
         println( "Test ==== Waiting for Matches ====")
         val ms = futTgt()
         println( "Test ==== Match Results ====")
         ms.foreach( println _ )
      }
   }

   def segmAndFillAndThen( fun: (Phrase, IIdxSeq[ OverwriteInstruction ], DifferanceDatabaseFiller) => Unit ) {
      val fill = atomic( "Test open db" ) { tx1 =>
         val d    = Database( new File( "audio_work", "database" ))( tx1 )
         val tv   = Television.fromFile( new File( new File( "audio_work", "test" ), "aljazeera1.aif" ))
         DifferanceDatabaseFiller( d, tv )( tx1 )
      }
      segmAndThen { (phrase, ovs) =>
         val fillFut = atomic( "Test fill db" ) { tx1 =>
            fill.perform( tx1 )
         }
         println( "Test ==== Waiting for Filling ====")
         fillFut()
         println( "Test ==== Filling done ====" )
         fun( phrase, ovs, fill )
      }
   }

   def segmAndThen( fun: (Phrase, IIdxSeq[ OverwriteInstruction ]) => Unit ) {
      val sel     = DifferanceOverwriteSelector()
      val (phrase, spanFut) = atomic( "Test phrase.fromFile" ) { implicit tx =>
         val p = Phrase.fromFile( new File( "/Users/hhrutz/Desktop/from_mnemo/Amazonas.aif" ))
         (p, sel.selectParts( p ))
      }
      // make a real one... Actor.actor {} produces a daemon actor,
      // which gets GC'ed and thus makes the VM prematurely quit
      new actors.Actor {
         start()
         def act() {
            println( "Test ==== Waiting for Overwrites ====")
            val ovs = spanFut.apply()
            println( "Test ==== Results ====")
            ovs.foreach( println _ )
            fun( phrase, ovs )
         }
      }
   }

   /**
    * Tests the `apply` method of `FutureResult` when it requires waiting for the result.
    */
   def future1() {
      import actors.{TIMEOUT, Actor}
      import Actor._

      actor {
         val ev = FutureResult.event[ Int ]()
         println( "Test spawing actor 2" )
         actor {
            reactWithin( 3000L ) {
               case TIMEOUT => ev.set( 33 )
            }
         }
         println( "Test entering await" )
         val res = ev.apply()
         println( "Test result = " + res )
      }
   }

   /**
    * Tests the `apply` method of `FutureResult` when it is instantly available.
    */
   def future2() {
      import actors.{TIMEOUT, Actor}
      import Actor._

      actor {
         val ev = FutureResult.event[ Int ]()
         println( "Test spawing actor 2" )
         actor {
            ev.set( 33 )
         }
         println( "Test sleeping" )
         receiveWithin( 3000L ) {
            case TIMEOUT =>
               println( "Test entering await" )
               val res = ev.apply()
               println( "Test result = " + res )
         }
      }
   }

   /**
    * Tests the `map` method of `FutureResult`.
    */
   def future3() {
      import actors.{TIMEOUT, Actor}
      import Actor._

      actor {
         val ev = FutureResult.event[ Int ]()
         val evP1 = ev.map( _ + 1 )
         actor {
            reactWithin( 3000L ) {
               case TIMEOUT => ev.set( 33 )
            }
         }
         println( "Test entering await" )
         val res = evP1.apply()
         println( "Test result = " + res )
      }
   }

   /**
    * Tests the `flatMap` method of `FutureResult`.
    */
   def future4() {
      import actors.{TIMEOUT, Actor}
      import Actor._

      actor {
         val ev = FutureResult.event[ Int ]()
         val evm = ev.flatMap { i =>
            val ev2 = FutureResult.event[ Int ]()
            actor {
               reactWithin( 3000L ) {
                  case TIMEOUT =>
                     println( "Test Setting ev2" )
                     ev2.set( i * i )
               }
            }
            ev2
         }
         actor {
            reactWithin( 3000L ) {
               case TIMEOUT =>
                  println( "Test Setting ev" )
                  ev.set( 33 )
            }
         }
         println( "Test entering await" )
         val res = evm.apply()
         println( "Test result = " + res )
      }
   }
}