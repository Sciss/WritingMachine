name := "writingmachine"

version := "0.10-SNAPSHOT"

organization := "de.sciss"

scalaVersion := "2.9.1"

libraryDependencies ++= Seq(
   "de.sciss" %% "nuagespompe" % "0.10-SNAPSHOT"
)

retrieveManaged := true

scalacOptions += "-deprecation"

