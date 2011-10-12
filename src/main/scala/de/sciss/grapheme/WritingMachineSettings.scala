package de.sciss.grapheme

import java.util.Properties
import java.io.{FileInputStream, File}

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
            case e => e.printStackTrace()
         }
      }
      prop
   }

   def getBool( name: String, default: Boolean ) : Boolean = {
      try {
         val v = properties.getProperty( name )
         if( v == null ) default else v.toBoolean
      } catch {
         case e => default
      }
   }

   def getInt( name: String, default: Int ) : Int = {
      try {
         val v = properties.getProperty( name )
         if( v == null ) default else v.toInt
      } catch {
         case e => default
      }
   }

   def getDouble( name: String, default: Double ) : Double = {
      try {
         val v = properties.getProperty( name )
         if( v == null ) default else v.toDouble
      } catch {
         case e => default
      }
   }
}