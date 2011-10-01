package de.sciss.grapheme

import impl.DifferanceSpatImpl
import de.sciss.nuages.{NuagesLauncher}

object Init {
   def apply( r: NuagesLauncher.Ready )( implicit tx: Tx ) : Init = {
      val coll = r.frame.panel.collector.getOrElse( sys.error( "Requires nuages to use collector" ))
      val spat = DifferanceSpatImpl( coll )
      new Init( spat )
   }
}
class Init private ( spat: DifferanceSpat ) {

}