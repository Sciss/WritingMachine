package de.sciss.grapheme

import java.io.File

object Tests {
   def main( args: Array[ String ]) {
      args.headOption match {
         case Some( "--fut1" ) => future1()
         case Some( "--fut2" ) => future2()
         case Some( "--fut3" ) => future3()
         case Some( "--fut4" ) => future4()
         case Some( "--segm" ) => segm()
         case _ => sys.error( "Illegal arguments : " + args )
      }
   }

   def segm() {
      import actors.Actor
      import Actor._

      val sel     = DifferanceOverwriteSelector()
      val spanFut = atomic( "Phrase.fromFile" ) { implicit tx =>
         val phrase = Phrase.fromFile( new File( "/Users/hhrutz/Desktop/from_mnemo/Amazonas.aif" ))
         sel.selectParts( phrase )
      }
      new Actor {
         start()
         def act() {
            println( "==== Waiting for Overwrites ====")
            val ovs = spanFut.apply()
            println( "==== Overwrite Instructions ====")
            ovs.foreach( println _ )
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
         println( "spawing actor 2" )
         actor {
            reactWithin( 3000L ) {
               case TIMEOUT => ev.set( 33 )
            }
         }
         println( "entering await" )
         val res = ev.apply()
         println( "result = " + res )
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
         println( "spawing actor 2" )
         actor {
            ev.set( 33 )
         }
         println( "sleeping" )
         receiveWithin( 3000L ) {
            case TIMEOUT =>
               println( "entering await" )
               val res = ev.apply()
               println( "result = " + res )
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
         println( "entering await" )
         val res = evP1.apply()
         println( "result = " + res )
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
                     println( "Setting ev2" )
                     ev2.set( i * i )
               }
            }
            ev2
         }
         actor {
            reactWithin( 3000L ) {
               case TIMEOUT =>
                  println( "Setting ev" )
                  ev.set( 33 )
            }
         }
         println( "entering await" )
         val res = evm.apply()
         println( "result = " + res )
      }
   }
}