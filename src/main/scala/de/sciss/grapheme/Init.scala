/*
 *  Init.scala
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

import de.sciss.nuages.{NuagesLauncher}
import java.io.File
import actors.{TIMEOUT, Actor}

object Init {
   import WritingMachine._

   private val instanceRef = Ref.empty[ Init ]

   def instance( implicit tx: Tx ) : Init = instanceRef()

   def apply( r: NuagesLauncher.Ready )( implicit tx: Tx ) : Init = {
      require( instanceRef() == null )
      val coll    = r.frame.panel.collector.getOrElse( sys.error( "Requires nuages to use collector" ))
      val spat    = DifferanceSpat( coll )
      val db      = Database( databaseDir )
//      val tv      = Television.fromFile( new File( testDir, "aljazeera1.aif" ))
      val tv      = Television.fromFile( new File( testDir, "mulholland_drive_ct.aif" ))
      val filler  = DifferanceDatabaseFiller( db, tv )
      val thinner = DifferanceDatabaseThinner( db )
      val trace   = PhraseTrace()
      val query   = DifferanceDatabaseQuery( db )
      val over    = DifferanceOverwriter()
      val overSel = DifferanceOverwriteSelector()
      val start   = Phrase.fromFile( new File( testDir, "amazonas_m.aif" ))
      val diff    = DifferanceAlgorithm( /* spat, */ thinner, filler, trace, query, over, overSel, start )
      val i       = new Init( start, spat, diff )
      instanceRef.set( i )
      i
   }
}
final class Init private ( _phrase0: Phrase, val spat: DifferanceSpat, val differance: DifferanceAlgorithm ) {
   import GraphemeUtil._

   val numSectors = WritingMachine.masterNumChannels

   var overlapMotion : Motion = Motion.exprand( 1.1, numSectors - 1 )

   var keepGoing = true

   private lazy val actor = new Actor {
      def act() {
         var p          = _phrase0 // atomic( "Init query current phrase" )( tx => differance.currentPhrase( tx ))
         val spatFuts   = Array.fill( numSectors )( futureOf( () ))
         var sector     = 0
         while( keepGoing ) {
            if( !spatFuts( sector ).isSet ) {
               logNoTx( "==== Init wait for busy spat sector " + (sector+1) + " ====" )
               spatFuts( sector )()
            }
            // differance process
            val (spatFut, stepFut) = atomic( "Init difference algorithm step" ) { tx =>
               val _spatFut = spat.rotateAndProject( p )( tx )
               val _stepFut = differance.step( tx )
               (_spatFut, _stepFut)
            }
            spatFuts( sector ) = spatFut
            val dur = atomic( "Init determining rotation duration" ) { tx =>
               val olap = overlapMotion.step( tx )
               framesToSeconds( p.length ) / olap
            }
            val t1      = System.currentTimeMillis()
            logNoTx( "==== Init wait for algorithm step ====" )
            p = stepFut()
            val dur2    = (System.currentTimeMillis() - t1) * 0.001
            val dur3    = dur - dur2
            if( dur3 > 0.0 ) {
               logNoTx( "==== Init waiting " + dur3 + " secs to next rotation ====" )
               receiveWithin( (dur3 * 1000).toLong ) {
                  case TIMEOUT =>
               }
            }

            sector = (sector + 1) % numSectors
         }
      }
   }

   def start() { actor.start() }
}