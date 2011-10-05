/*
 *  DifferanceDatabaseQueryImpl.scala
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
package impl

import de.sciss.strugatzki.FeatureCorrelation
import java.io.File

object DifferanceDatabaseQueryImpl {
   def apply( db: Database ) : DifferanceDatabaseQuery = new DifferanceDatabaseQueryImpl( db )
}
class DifferanceDatabaseQueryImpl private ( db: Database ) extends AbstractDifferanceDatabaseQuery {
   import DifferanceDatabaseQuery._

   val matchDurationMotion    = Motion.exprand( 0.4, 4.0 )
   val matchDeviationMotion   = Motion.linrand( 0.2, 0.5 )
   val spectralMotion         = Motion.linrand( 0.25, 0.75 )
   val stretchDeviationMotion = Motion.linrand( 0.2, 0.5 )
   val rankMotion             = Motion.linrand( 0, 11 )

   def findMatch( rank: Int, phrase: Phrase, punchIn: Span, punchOut: Span,
                  minPunch: Long, maxPunch: Long, weight: Double )( implicit tx: Tx ) : FutureResult[ Match ] = {

      db.asStrugatziDatabase.flatMap( dir =>
         findMatchIn( dir, rank, phrase, punchIn, punchOut, minPunch, maxPunch, weight ))
   }

   private def findMatchIn( dir: File, rank: Int, phrase: Phrase, punchIn: Span, punchOut: Span,
                  minPunch: Long, maxPunch: Long, weight: Double ) : FutureResult[ Match ] = {

      val res              = FutureResult.event[ Match ]()
      val set              = FeatureCorrelation.SettingsBuilder()
      set.databaseFolder   = dir
      set.maxBoost         = sys.error( "TODO" )
      set.maxPunch         = sys.error( "TODO" )
      set.metaInput        = sys.error( "TODO" )
      set.minPunch         = sys.error( "TODO" )
      set.minSpacing       = sys.error( "TODO" )
      set.normalize        = sys.error( "TODO" )
      set.numMatches       = sys.error( "TODO" )
      set.numPerFile       = sys.error( "TODO" )
      set.punchIn          = sys.error( "TODO" )
      set.punchOut         = sys.error( "TODO" )

      val fc               = FeatureCorrelation( set ) {
         case FeatureCorrelation.Aborted =>
            println( "DifferanceDatabaseQuery : Ouch. Aborted. Need to handle this case!" )
            res.set( Match( Span( 0L, min( db.length, secondsToFrames( 1.0 ))), 1f, 1f ))

         case FeatureCorrelation.Failure( e ) =>
            println( "DifferanceDatabaseQuery : Ouch. Failure. Need to handle this case!" )
            e.printStackTrace()
            res.set( Match( Span( 0L, min( db.length, secondsToFrames( 1.0 ))), 1f, 1f ))

         case FeatureCorrelation.Success( coll ) =>
            println( "DifferanceDatabaseQuery : Ouch. Success. Need to handle this case!" )
            res.set( Match( Span( 0L, min( db.length, secondsToFrames( 1.0 ))), 1f, 1f ))

//            val b    = if( coll( 0 ).sim.isNaN ) coll( 1 ) else coll( 0 )
//            val len  = atomic( "DifferanceDatabaseQuery : success" ) { tx2 =>
//               (random( tx2 ) * (maxLen - minLen) + minLen).toLong
//            }
//            val s    = if( b.pos <= center ) {
//               val stop0   = max( center, b.pos + len/2 )
//               val start   = max( 0L, stop0 - len )
//               val stop    = min( spec.numFrames, start + len )
//               Span( start, stop )
//            } else {
//               val start0  = min( center, b.pos - len/2 )
//               val stop    = min( spec.numFrames, start0 + len )
//               val start   = max( 0L, stop - len )
//               Span( start, stop )
//            }
//            res.set( s )

         case FeatureCorrelation.Progress( p ) =>
      }
      fc.start()
      res
   }
}