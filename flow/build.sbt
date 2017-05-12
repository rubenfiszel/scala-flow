name := "flow"

organization := "dawn"

version := "0.1"

scalaVersion := "2.12.1"

libraryDependencies += "org.scalanlp"  %% "breeze" % "0.13.1"
libraryDependencies += "org.scalanlp"  %% "breeze-viz" % "0.13.1"
libraryDependencies += "org.typelevel" %% "spire"  % "0.14.1"
//libraryDependencies += "org.typelevel" %% "cats" % "0.9.0"

resolvers += "jzy3d-snapshots" at "http://maven.jzy3d.org/releases"

libraryDependencies += "org.jzy3d" % "jzy3d-api"  % "1.0.0"

val circeVersion = "0.7.0"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

addCompilerPlugin(
  "org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full
)
