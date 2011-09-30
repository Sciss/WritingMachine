name            := "writingmachine"

appbundleName   := "WritingMachine"

organization    := "de.sciss"

version         := "0.10-SNAPSHOT"

scalaVersion    := "2.9.1"

libraryDependencies ++= Seq(
   "de.sciss" %% "nuagespompe" % "0.10-SNAPSHOT"
)

retrieveManaged := true

scalacOptions += "-deprecation"

seq( appbundleSettings: _* )
