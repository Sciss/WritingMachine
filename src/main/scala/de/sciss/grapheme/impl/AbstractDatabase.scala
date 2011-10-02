package de.sciss.grapheme
package impl

import collection.immutable.{IndexedSeq => IIdxSeq}

abstract class AbstractDatabase extends Database with GraphemeUtil {
   /**
    * Maximum extension of removal spans to each side, in seconds.
    */
   def removalMarginMotion : Motion

   /**
    * Spectral weight in removal (0 = temporal breaks, 1 = spectral breaks,
    * 0.5 = mix of both).
    */
   def removalSpectralMotion : Motion

   /**
    * Fading duration in seconds during removal.
    */
   def removalFadeMotion: Motion

   def bestRemoval( span: Span, margin: Long, weight: Double, fade: Long )( implicit tx: Tx ) : RemovalInstruction

   def performRemovals( instrs: IIdxSeq[ RemovalInstruction ])( implicit tx: Tx ) : Unit

   def remove( spans: IIdxSeq[ Span ])( implicit tx: Tx ) {
      val margin  = secondsToFrames( removalMarginMotion.step )
      val spect   = removalSpectralMotion.step
      val fade    = secondsToFrames( removalFadeMotion.step )
      val instrs  = spans.map( span => bestRemoval( span, margin, spect, fade ))
      val merged  = instrs.headOption match {
         case Some( ih ) =>
            instrs.drop( 1 ).foldLeft( IIdxSeq( ih ))( (res, i) => {
               val (ov, not)  = res.partition( _.span.touches( i.span ))
               val union      = ov.foldLeft( i )( (a, b) => {
                  val start   = min( a.span.start, b.span.start)
                  val stop    = max( a.span.stop, b.span.stop)
                  val fadeIn  = if( start == a.span.start ) a.fadeIn else b.fadeIn
                  val fadeOut = if( stop == a.span.stop ) a.fadeOut else b.fadeOut
                  RemovalInstruction( Span( start, stop ), fadeIn, fadeOut )
               })
               not :+ union
            })
         case None => IIdxSeq.empty
      }
      val sorted  = merged.sortBy( _.span.start )
      performRemovals( sorted )
   }
}