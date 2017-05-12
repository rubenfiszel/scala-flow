organization in ThisBuild := "dawn"

version in ThisBuild := "0.1"

scalaVersion in ThisBuild := "2.12.1"

lazy val root = (project in file("core"))

lazy val drone = project
  .dependsOn(root)

