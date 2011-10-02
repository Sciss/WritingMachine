package de.sciss.grapheme

import collection.immutable.{IndexedSeq => IIdxSeq}
import de.sciss.synth.io.AudioFile

trait Database {
   def length: Long

//   def randomPhrase( length: Long )( implicit tx: Tx ) : Phrase
   def remove( spans: IIdxSeq[ Span ])( implicit tx: Tx ) : Unit
   def append( source: AudioFile, length: Long )( implicit tx: Tx ) : Unit
}