package de.sciss.grapheme
package impl

import java.io.File
import de.sciss.strugatzki.FeatureExtraction

trait ExtractionImpl {
   import GraphemeUtil._

   /**
    * Starts an extraction process for a given audio file input. The process
    * is started after the transaction commits, and the method returns
    * the future result of the meta file thus generated.
    */
   protected def extract( audioInput : File, dir: Option[ File ])( implicit tx: Tx ) : FutureResult[ File ] = {
      val res = FutureResult.event[ File ]()
      tx.afterCommit { _ =>
         import FeatureExtraction._
         val set           = SettingsBuilder()
         set.audioInput    = audioInput
         set.featureOutput = createTempFile( ".aif", dir )
         val meta          = createTempFile( "_feat.xml", dir )
         set.metaOutput    = Some( meta )
         val process       = apply( set ) {
            case Aborted =>
               println( "Extraction : Ouch : Aborted. Need to handle this case!" )
               res.set( meta )

            case Failure( e ) =>
               println( "Extraction : Ouch. Failure. Need to handle this case!" )
               e.printStackTrace()
               res.set( meta )

            case Success( _ ) =>
               res.set( meta )

            case Progress( p ) =>
         }
         process.start()
      }
      res
   }
}