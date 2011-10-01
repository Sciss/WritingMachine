package de.sciss.grapheme

import de.sciss.synth.proc.Proc

trait Phrase {
   def length: Long
   def player( implicit tx: Tx ) : Proc
}