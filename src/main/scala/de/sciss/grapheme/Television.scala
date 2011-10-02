package de.sciss.grapheme

import java.io.File

trait Television {
   def capture( length: Long )( implicit tx: Tx ) : Future[ File ]
}