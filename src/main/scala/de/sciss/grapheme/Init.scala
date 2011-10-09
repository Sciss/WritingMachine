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

import java.io.File
import actors.{TIMEOUT, Actor}
import de.sciss.synth.{Server, AudioBus}
import de.sciss.synth
import synth.osc.OSCResponder
import de.sciss.osc.Message
import swing.Swing
import de.sciss.nuages.{ControlPanel, NuagesLauncher}

object Init {
   import WritingMachine._

   private val instanceRef = Ref.empty[ Init ]

   def instance( implicit tx: Tx ) : Init = instanceRef()

   def apply( r: NuagesLauncher.Ready )( implicit tx: Tx ) : Init = {
      require( instanceRef() == null )

      val panel   = r.frame.panel
      val coll    = panel.collector.getOrElse( sys.error( "Requires nuages to use collector" ))
      val spat    = DifferanceSpat( coll )
//      val tv      = Television.fromFile( new File( testDir, "aljazeera1.aif" ))
      val tv      = if( tvUseTestFile ) {
         Television.fromFile( new File( testDir, "mulholland_drive_ct.aif" ))
      } else {
         Television.live()
      }
//      val filler  = DifferanceDatabaseFiller( db, tv )
//      val thinner = DifferanceDatabaseThinner( db )
//      val trace   = PhraseTrace()
//      val query   = DifferanceDatabaseQuery( db )
//      val over    = DifferanceOverwriter()
//      val overSel = DifferanceOverwriteSelector()
////      val start   = Phrase.fromFile( new File( testDir, "amazonas_m.aif" ))
//      val diff    = DifferanceAlgorithm( /* spat, */ thinner, filler, trace, query, over, overSel, start )
      val i       = new Init( /* start, diff, */ spat, tv )
      instanceRef.set( i )

      tx.afterCommit { _ =>
         val s = Server.default
         startMeterSynth(
            r.controlPanel,
            AudioBus( s, s.options.outputBusChannels + tvChannelOffset, tvNumChannels ),
            panel.masterBus.get
         )
         if( autoStart ) Swing.onEDT( r.controlPanel.startClock() )
      }
      i
   }

   private def startMeterSynth( ctrl: ControlPanel, inBus: AudioBus, outBus: AudioBus ) {
      import synth._
      import ugen._

      val s = inBus.server

      val df = SynthDef( "post-master" ) {
         val inSig         = In.ar( inBus.index, inBus.numChannels )
         val outSig        = In.ar( outBus.index, outBus.numChannels )
         val sig           = Flatten( Seq( outSig, inSig ))
         val meterTr       = Impulse.kr( 20 )
         val peak          = Peak.kr( sig, meterTr )
         val rms           = A2K.kr( Lag.ar( sig.squared, 0.1 ))
         val meterData     = Zip( peak, rms )  // XXX correct?
         SendReply.kr( meterTr, meterData, "/meters" )
      }
      val syn     = df.play( s, addAction = addToTail )
      val synID   = syn.id
      OSCResponder.add({
         case Message( "/meters", `synID`, 0, values @ _* ) =>
            Swing.onEDT { ctrl.meterUpdate( values.map( _.asInstanceOf[ Float ])( collection.breakOut ))}
      }, s  )
   }
}
final class Init private ( /* _phrase0: Phrase, val differance: DifferanceAlgorithm, */ val spat: DifferanceSpat,
                            val tv: Television ) {
   import GraphemeUtil._

   val numSectors = WritingMachine.masterNumChannels

   var overlapMotion : Motion = Motion.exprand( 1.1, numSectors - 1 )

   var keepGoing = true

   private lazy val actor = new Actor {
      def act() {
//      val start   = Phrase.fromFile( new File( testDir, "amazonas_m.aif" ))
//         var p          = _phrase0 // atomic( "Init query current phrase" )( tx => differance.currentPhrase( tx ))
         val futP0   = atomic( "meta-diff initial tv capture" )( tx => tv.capture( secondsToFrames( 4.0 ))( tx ))
         logNoTx( "==== meta-diff wait for initial tv capture ====" )
         val fP0     = futP0.apply()
//         logNoTx( "==== meta-diff get first phrase ====" )
         val (differance, _p0) = atomic( "meta-diff initialize algorithm" ) { tx =>
            val p0      = Phrase.fromFile( fP0 )( tx )
            val db      = Database( databaseDir )( tx )
            val filler  = DifferanceDatabaseFiller( db, tv )( tx )
            val thinner = DifferanceDatabaseThinner( db )
            val trace   = PhraseTrace()
            val query   = DifferanceDatabaseQuery( db )
            val over    = DifferanceOverwriter()
            val overSel = DifferanceOverwriteSelector()
            (DifferanceAlgorithm( /* spat, */ thinner, filler, trace, query, over, overSel, p0 ), p0)
         }

         var p          = _p0
         val spatFuts   = Array.fill( numSectors )( futureOf( () ))
         var sector     = 0
         while( keepGoing ) {
            if( !spatFuts( sector ).isSet ) {
               logNoTx( "==== meta-diff wait for busy spat sector " + (sector+1) + " ====" )
               spatFuts( sector )()
            }
            // differance process
            val (spatFut, stepFut) = atomic( "meta-diff difference algorithm step" ) { tx =>
               val _spatFut = spat.rotateAndProject( p )( tx )
               val _stepFut = differance.step( tx )
               (_spatFut, _stepFut)
            }
            spatFuts( sector ) = spatFut
            val dur = atomic( "meta-diff determining rotation duration" ) { tx =>
               val olap = overlapMotion.step( tx )
               framesToSeconds( p.length ) / olap
            }
            val t1      = System.currentTimeMillis()
            logNoTx( "==== meta-diff wait for algorithm step ====" )
            p = stepFut()
            val dur2    = (System.currentTimeMillis() - t1) * 0.001
            val dur3    = dur - dur2
            if( dur3 > 0.0 ) {
               logNoTx( "==== meta-diff waiting " + dur3 + " secs to next rotation ====" )
               receiveWithin( (dur3 * 1000).toLong ) {
                  case TIMEOUT =>
               }
            }
//            atomic( "meta-diff releasing spat phrase" ) { tx => spat.releasePhrase( tx )}

            sector = (sector + 1) % numSectors
         }
      }
   }

   def start() { actor.start() }
   def stop() {
      warnToDo( "Init : stop" )
   }
}