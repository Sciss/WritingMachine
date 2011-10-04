//package de.sciss.grapheme
//
//import actors.{Channel, Actor}
//import de.sciss.synth.aux.{FutureActor, RevocableFuture}
//import concurrent.SyncVar
//
//object Futures {
//   sealed trait Result[ +A ]
//   sealed trait FailureOrCancelled extends Result[ Nothing ]
//   final case class Failure( reason: AnyRef ) extends FailureOrCancelled
//   final case class Success[ A ]( value: A ) extends Result[ A ]
//   case object Cancelled extends FailureOrCancelled
//
//   sealed trait State {
//      def shouldCancel: Boolean
//   }
//
//   sealed trait ResolvableFuture[ A ] {
//      def resolve( res: Result[ A ]) : Unit
//   }
//
//   def event[ A ] : RevocableFuture[ Result[ A ]] with State with ResolvableFuture[ A ] = {
//      val c = new Channel[ Result[ A ]]( Actor.self )
//      val a = new FutureActor[ Result[ A ]]( c ) with State with ResolvableFuture[ A ] {
//         futureActor =>
//
//         val sync    = new AnyRef
//         @volatile var revoked = false
//         var result : Either[ SyncVar[ Result[ A ]], Option[ Result[ A ]]] = Right( None )
//
//         def shouldCancel = revoked
//
//         def resolve( res: Result[ A ]) {
//            sync.synchronized {
//               result match {
//                  case Left( sv )            => sv.set( res )
//                  case Right( None )         => result = Right( Some( res ))
//                  case Right( Some( res2 ))  => sys.error( "Was already resolved (" + res2 + ")" )
//               }
//            }
//         }
//
//         def body( sv: SyncVar[ Result[ A ]]) {
//            sync.synchronized {
//               result match {
//                  case Right( Some( res )) => sv.set( res )
//                  case _ =>
//               }
//               result = Left( sv )
//            }
//         }
//         def revoke() { sync.synchronized {
//            revoked = true
//         }}
//      }
//      a.start()
//      a
//   }
//
//   def thread[ A ]( thunk: State => Result[ A ]) : RevocableFuture[ Result[ A ]] = {
//      val c = new Channel[ Result[ A ]]( Actor.self )
//      val a = new FutureActor[ Result[ A ]]( c ) with State {
//         futureActor =>
//
//         val sync    = new AnyRef
//         @volatile var revoked = false
////         var thread  = Option.empty[ Thread ]
//
//         def shouldCancel = revoked
//
//         def body( res: SyncVar[ Result[ A ]]) {
//            val futCh   = new Channel[ Result[ A ]]( Actor.self )
//            sync.synchronized { if( !revoked ) {
//               val th = new Thread {
//                  override def run() {
//                     val res: Result[ A ] = try {
//                        thunk( futureActor )
//                     } catch {
//                        case e =>
//                           e.printStackTrace()
//                           Failure( e )
//                     }
//                     futCh ! res
//                  }
//               }
//               th.setDaemon( true )
//               th.start()
////               thread = Some( th )
//            }}
//            futCh.react { case r => res.set( r )}
//         }
//         def revoke() { sync.synchronized {
//            revoked = true
////            thread.foreach {}
////            oh.foreach( OSCReceiverActor.removeHandler( _ ))
////            oh = None
//         }}
//      }
//      a.start()
//      a
//   }
//
//   def test1() {
//      testThread( false )
//   }
//
//   private def testThread( cancel: Boolean ) {
//      val a = new Actor {
//         def act() {
//            println( "Spawing thread" )
//            val fut = thread { state =>
//               var i = 0; while( i < 10 && !state.shouldCancel ) {
//                  println( "Thread working" )
//                  Thread.sleep( 1000 )
//                  i += 1
//               }
//               if( state.shouldCancel ) Cancelled else Success( 33 )
//            }
//
//            println( "Awaiting thread result" )
//            val res = await( fut )
//            println( "Thread returned " + res )
//         }
//      }
//      a.start()
//   }
//
//   def await[ A ]( fut: Future[ Result[ A ]]) : Result[ A ] = {
//      if( fut.isSet ) fut() else {
////         val me = Actor.self
////         fut.inputChannel.react {
////            case res: Result[ _ ] =>
////               println( "Aqui" )
////               me ! res
////         }
//         fut.inputChannel.receive {
//            case res: Result[ _ ] =>
//               println( "Aqui" )
//               res   // why don't we need asInstanceOf ?
//         }
////         me receive {
////            case res: Result[ _ ] =>
////               println( "Zwei" )
////               res.asInstanceOf[ Result[ A ]]
////         }
//      }
//   }
//}