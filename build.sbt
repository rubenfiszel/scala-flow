organization in ThisBuild := "dawn"

version in ThisBuild := "0.1"

scalaVersion in ThisBuild := "2.12.1"

val paradiseVersion = "2.1.0"

val commonSettings = Seq(
  //paradise
  resolvers += Resolver.sonatypeRepo("snapshots"),
  resolvers += Resolver.sonatypeRepo("releases"),
  addCompilerPlugin("org.scalamacros" % "paradise" % paradiseVersion cross CrossVersion.full)
)

lazy val root = (project in file("core"))
  .settings(commonSettings)

lazy val drone = project
  .settings(commonSettings)
  .dependsOn(root)

