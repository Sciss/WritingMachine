package de.sciss

import synth.proc.ProcTxn

package object grapheme {
   type Tx = ProcTxn
   type Future[ T ] = scala.actors.Future[ T ]
}