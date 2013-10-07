import sbt._
import sbt.Keys._

object NoaaIsdLiteBuild extends Build {

  lazy val noaaIsdLite = Project(
    id = "noaa-isd-lite",
    base = file("."),
    settings = Project.defaultSettings ++ Seq(
      name := "NOAA ISD Lite",
      organization := "us.tanimoto",
      version := "0.1-SNAPSHOT",
      scalaVersion := "2.10.3",
      libraryDependencies ++= Seq(
        "commons-net" % "commons-net" % "3.3",
        "com.jsuereth" %% "scala-arm" % "1.3",

        // Test
        "org.scalatest" % "scalatest_2.10" % "2.0.RC1" % "test"
      )
    )
  )
}
