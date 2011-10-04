package de.sciss.grapheme

import de.sciss.synth.proc.Proc

object RichProc {
   def apply( proc: Proc ) : RichProc = new Impl( proc )

   private final class Impl( proc: Proc ) extends RichProc {
      def futureStopped( implicit tx: Tx ) : FutureResult[ Unit ] = {
         require( proc.isPlaying )
         val fut = FutureResult.event[ Unit ]()
         lazy val l: Proc.Listener = new Proc.Listener {
            def updated( u: Proc.Update ) {
               if( !u.state.playing ) {
                  atomic( "Proc : futureStopped listener" ) { implicit tx =>
                     proc.removeListener( l )
                     fut.set( () )
                  }
               }
            }
         }
         proc.addListener( l )
         fut
      }
   }
}
sealed trait RichProc {
   def futureStopped( implicit tx: Tx ) : FutureResult[ Unit ]
}