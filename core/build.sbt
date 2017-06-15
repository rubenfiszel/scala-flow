name := "flow-core"

resolvers += "jzy3d-snapshots" at "http://maven.jzy3d.org/releases"

libraryDependencies ++= Seq(
  "org.scalanlp"  %% "breeze" % "0.13.1"
    , "org.scalanlp"  %% "breeze-viz" % "0.13.1"
    , "org.typelevel" %% "spire"  % "0.14.1"
    , "org.jzy3d" % "jzy3d-api"  % "1.0.0"
    , "com.github.mdr" %% "ascii-graphs" % "0.0.6"
    //  , "com.chuusai" %% "shapeless" % "2.3.2"
)
//resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/releases"
//Need to install locally (clone and sbt publish-local) for now ... https://github.com/mdr/ascii-graphs/



val circeVersion = "0.7.0"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

addCompilerPlugin(
  "org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full
)
