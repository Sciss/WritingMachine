package de.sciss.grapheme

object DifferanceDatabaseQuery {
   final case class Match( span: Span, boostIn: Float, boostOut: Float )
}
trait DifferanceDatabaseQuery {
   import DifferanceDatabaseQuery._

   def find( phrase: Phrase, overwrite: OverwriteInstruction )( implicit tx: Tx ) : Match
}