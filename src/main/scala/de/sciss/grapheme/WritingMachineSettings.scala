/*
 *  WritingMachineSettings.scala
 *  (WritingMachine)
 *
 *  Copyright (c) 2011-2017 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.grapheme

import java.util.Properties
import java.io.{File, FileInputStream}

import scala.util.control.NonFatal

class WritingMachineSettings {
  private[this] val properties = {
    val file = new File("writingmachine-settings.xml")
    val prop = new Properties()
    if (file.isFile) {
      try {
        val is = new FileInputStream(file)
        try {
          prop.loadFromXML(is)
        } finally {
          is.close()
        }
      } catch {
        case NonFatal(e) => e.printStackTrace()
      }
    }
    prop
  }

  def getString(name: String, default: String): String = {
    try {
      val v = properties.getProperty(name)
      if (v == null) default else v
    } catch {
      case NonFatal(_) => default
    }
  }

  def getBool(name: String, default: Boolean): Boolean = {
    try {
      val v = properties.getProperty(name)
      if (v == null) default else v.toBoolean
    } catch {
      case NonFatal(_) => default
    }
  }

  def getInt(name: String, default: Int): Int = {
    try {
      val v = properties.getProperty(name)
      if (v == null) default else v.toInt
    } catch {
      case NonFatal(_) => default
    }
  }

  def getDouble(name: String, default: Double): Double = {
    try {
      val v = properties.getProperty(name)
      if (v == null) default else v.toDouble
    } catch {
      case NonFatal(_) => default
    }
  }
}