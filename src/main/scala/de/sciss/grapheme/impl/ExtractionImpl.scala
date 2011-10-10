package de.sciss.grapheme
package impl

import java.io.File
import de.sciss.strugatzki.FeatureExtraction

trait ExtractionImpl {
   import GraphemeUtil._

   protected def identifier: String

   /**
    * Starts an extraction process for a given audio file input. The process
    * is started after the transaction commits, and the method returns
    * the future result of the meta file thus generated.
    */
   protected def extract( audioInput : File, dir: Option[ File ], keep: Boolean )( implicit tx: Tx ) : FutureResult[ File ] = {
      val res = FutureResult.event[ File ]()
      tx.afterCommit { _ =>
         import FeatureExtraction._
         val set           = SettingsBuilder()
         set.audioInput    = audioInput
         set.featureOutput = createTempFile( ".aif", dir, keep )
         val meta          = createTempFile( "_feat.xml", dir, keep )
         set.metaOutput    = Some( meta )
         val process       = apply( set ) {
            case Aborted =>
               val e = new RuntimeException( identifier + " process aborted" )
               res.fail( e )

            case Failure( e ) =>
               res.fail( e )

            case Success( _ ) =>
               res.succeed( meta )

            case Progress( p ) =>
         }
         process.start()
      }
      res
   }
}