name            := "writingmachine"

appbundleName   := "WritingMachine"

organization    := "de.sciss"

version         := "0.10-SNAPSHOT"

scalaVersion    := "2.9.1"

resolvers += "Clojars Repository" at "http://clojars.org/repo"

libraryDependencies ++= Seq(
   "de.sciss" %% "nuagespompe" % "0.10-SNAPSHOT",
   "de.sciss" %% "strugatzki" % "0.13"
)

retrieveManaged := true

scalacOptions ++= Seq( "-deprecation", "-unchecked" )

seq( appbundleSettings: _* )
