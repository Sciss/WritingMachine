package de.sciss.grapheme

object Tests {
   def main( args: Array[ String ]) {
      args.headOption match {
         case Some( "--fut1" ) => future1()
         case Some( "--fut2" ) => future2()
         case _ => sys.error( "Illegal arguments : " + args )
      }
   }

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
}