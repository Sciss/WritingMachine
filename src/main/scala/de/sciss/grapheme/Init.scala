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

object Init {
   import WritingMachine._

   private val instanceRef = Ref.empty[ Init ]

   def instance( implicit tx: Tx ) : Init = instanceRef()

   def apply( r: NuagesLauncher.Ready )( implicit tx: Tx ) : Init = {
      require( instanceRef() == null )
      val coll    = r.frame.panel.collector.getOrElse( sys.error( "Requires nuages to use collector" ))
      val spat    = DifferanceSpat( coll )
      val db      = Database( databaseDir )
      val tv      = Television.fromFile( new File( testDir, "aljazeera1.aif" ))
      val filler  = DifferanceDatabaseFiller( db, tv )
      val thinner = DifferanceDatabaseThinner( db )
      val trace   = PhraseTrace()
      val query   = DifferanceDatabaseQuery( db )
      val over    = DifferanceOverwriter()
      val overSel = DifferanceOverwriteSelector()
      val start   = Phrase.fromFile( new File( testDir, "amazonas_m.aif" ))
      val diff    = DifferanceAlgorithm( spat, thinner, filler, trace, query, over, overSel, start )
      val i       = new Init( diff )
      instanceRef.set( i )
      i
   }
}
final class Init private ( val differance: DifferanceAlgorithm ) {
}