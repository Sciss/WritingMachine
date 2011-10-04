/*
 *  FutureResult.scala
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

import collection.immutable.{IndexedSeq => IIdxSeq}
import actors.{Futures, Actor, Future}

trait FutureResult[ A ] {
   def isSet : Boolean
   def apply() : A
   def respond( fun: A => Unit ) : Unit
   def map[ B ]( fun: A => B ) : FutureResult[ B ]
   def flatMap[ B ]( fun: A => FutureResult[ B ]) : FutureResult[ B ]
   private[grapheme] def peer : Future[ A ]
}

object FutureResult {
   def enrich[ A ]( f: IIdxSeq[ FutureResult[ A ]]) : FutureResult[ IIdxSeq[ A ]] = sys.error( "TODO" )
   def now[ A ]( value: A )( implicit tx: Tx ) : FutureResult[ A ] = sys.error( "TODO" )
   def unitSeq( fs: FutureResult[ _ ]* ) : FutureResult[ Unit ] = sys.error( "TODO" )
//   def event[ A ]() : Event[ A ] = Impl.event[ A ]()

   trait Event[ A ] extends FutureResult[ A ] {
      def set( result: A ) : Unit
   }

   def event[ A ]() : FutureResult.Event[ A ] = new FutureResult.Event[ A ] with Basic[ A ] {
      final case class Set( value : A )
      case object Get

      val act = Actor.actor {
         import Actor._
         react {
            case set @ Set( value ) => react {
               case Get => reply( set )
            }
         }
      }

      val peer = Futures.future {
         import Actor._
         act ! Get
         receive {
            case Set( value ) => value
         }
      }

      def set( value: A ) { act ! Set( value )}
   }

   def wrap[ A ]( fut: Future[ A ]) : FutureResult[ A ] = new FutureResult[ A ] with Basic[ A ] {
      def peer = fut
   }

   private sealed trait Basic[ A ] {
      me: FutureResult[ A ] =>

      def map[ B ]( fun: A => B ) : FutureResult[ B ] = wrap( Futures.future {
         fun( me.peer.apply() )
      })

      def flatMap[ B ]( fun: A => FutureResult[ B ]) : FutureResult[ B ] = wrap( Futures.future {
         fun( me.peer.apply() ).peer.apply()
      })

      def isSet : Boolean = peer.isSet

      def apply() : A = peer.apply()

      def respond( fun: A => Unit ) { peer.respond( fun )}
   }
}