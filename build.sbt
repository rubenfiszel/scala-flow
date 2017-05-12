scalaVersion in ThisBuild := "2.12.1"

lazy val flow = project

lazy val drone = project
  .dependsOn(flow)

