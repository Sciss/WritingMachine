package de.sciss.grapheme

import de.sciss.nuages.NuagesLauncher

object WritingMachine {
   def main( args: Array[ String ]) {
      launch()
   }

   def launch() {
      val cfg = NuagesLauncher.SettingsBuilder()
      cfg.beforeShutdown = () => println( "Bye bye..." )
      NuagesLauncher( cfg )
   }
}