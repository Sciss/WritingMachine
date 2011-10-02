package de.sciss.grapheme

import java.io.File
import de.sciss.synth.io.{SampleFormat, AudioFileType, AudioFileSpec, AudioFile}

trait GraphemeUtil {
   protected final def random( implicit tx: Tx ) : Double = math.random // XXX
   protected final def sampleRate : Double = 44100.0

   protected final def random( top: Int )( implicit tx: Tx ) : Int = (random * top).toInt
   protected final def secondsToFrames( secs: Double ) : Long = (secs * sampleRate + 0.5).toLong
   protected final def max( i: Int, is: Int* ) : Int = is.foldLeft( i )( _ max _ )
   protected final def max( n: Long, ns: Long* ) : Long = ns.foldLeft( n )( _ max _ )
   protected final def max( d: Double, ds: Double* ) : Double = ds.foldLeft( d )( _ max _ )
   protected final def min( i: Int, is: Int* ) : Int = is.foldLeft( i )( _ min _ )
   protected final def min( n: Long, ns: Long* ) : Long = ns.foldLeft( n )( _ min _ )
   protected final def min( d: Double, ds: Double* ) : Double = ds.foldLeft( d )( _ min _ )

   protected final def openMonoWrite( f: File ) : AudioFile =
      AudioFile.openWrite( f, AudioFileSpec( AudioFileType.IRCAM, SampleFormat.Float, 1, sampleRate ))
}