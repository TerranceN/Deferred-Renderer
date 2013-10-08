import AssemblyKeys._

assemblySettings

name := "LWJGL Project"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.10.1"

seq(lwjglSettings: _*)

scalacOptions ++= Seq("-unchecked", "-deprecation")

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % "2.10.1"
)
