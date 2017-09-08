package de.sciss.grapheme

import java.util.Properties
import java.io.{File, FileInputStream}

import scala.util.control.NonFatal

class WritingMachineSettings {
   private val properties = {
      val file = new File( "writingmachine-settings.xml" )
      val prop = new Properties()
      if( file.isFile ) {
         try {
            val is = new FileInputStream( file )
            try {
               prop.loadFromXML( is )
            } finally {
               is.close()
            }
         } catch {
            case NonFatal(e) => e.printStackTrace()
         }
      }
      prop
   }

   def getString( name: String, default: String ) : String = {
      try {
         val v = properties.getProperty( name )
         if( v == null ) default else v
      } catch {
         case NonFatal(_) => default
      }
   }

   def getBool( name: String, default: Boolean ) : Boolean = {
      try {
         val v = properties.getProperty( name )
         if( v == null ) default else v.toBoolean
      } catch {
         case NonFatal(_) => default
      }
   }

   def getInt( name: String, default: Int ) : Int = {
      try {
         val v = properties.getProperty( name )
         if( v == null ) default else v.toInt
      } catch {
         case NonFatal(_) => default
      }
   }

   def getDouble( name: String, default: Double ) : Double = {
      try {
         val v = properties.getProperty( name )
         if( v == null ) default else v.toDouble
      } catch {
         case NonFatal(_) => default
      }
   }
}