/*
 *  AbstractDifferanceSpat.scala
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

import de.sciss.synth.proc.{DSL, Proc, Ref}

abstract class AbstractDifferanceSpat extends DifferanceSpat {
   private val identifier = "a-differance-spat"

   def numChannels : Int
   def diffusion( chan: Int )( implicit tx: Tx ) : Proc

   private val chanRef = Ref( -1 )

   def rotateAndProject( phrase: Phrase )( implicit tx: Tx ) : FutureResult[ Unit ] = {
      val chan = (chanRef() + 1) % numChannels
      chanRef.set( chan )

      import DSL._
      val pProc   = phrase.player
      val pDiff   = diffusion( chan )
      pProc ~> pDiff
      pProc.play
      pProc.futureStopped.map { _ =>
         atomic( identifier + " : dispose phrase process" ) { implicit tx =>
            pProc.remove
         }
      }
   }
}