name            := "WritingMachine"
organization    := "de.sciss"
version         := "0.2.0-SNAPSHOT"
scalaVersion    := "2.12.3"
licenses        := Seq("GPL v3+" -> url("http://www.gnu.org/licenses/gpl-3.0.txt"))

scalacOptions  ++= Seq("-deprecation", "-unchecked", "-feature", "-encoding", "utf8", "-Xfuture", "-Xlint")

resolvers       += "Oracle Repository" at "http://download.oracle.com/maven"  // required for sleepycat

libraryDependencies ++= Seq(
  "de.sciss" %% "wolkenpumpe" % "2.18.0",
  "de.sciss" %% "strugatzki"  % "2.16.0",
  "de.sciss" %% "span"        % "1.3.3",
  "de.sciss" %  "submin"      % "0.2.1"
)
