import AssemblyKeys._

assemblySettings

name := "LWJGL Project"

version := "1.0"

scalaVersion := "2.10.1"

scalacOptions ++= Seq("-unchecked", "-deprecation")

retrieveManaged := true

jarsToExtract ++= Seq(
    "lib_managed/jars/org.lwjgl.lwjgl/lwjgl-platform/lwjgl-platform-2.9.0-natives-linux.jar",
    "lib_managed/jars/org.lwjgl.lwjgl/lwjgl-platform/lwjgl-platform-2.9.0-natives-osx.jar",
    "lib_managed/jars/org.lwjgl.lwjgl/lwjgl-platform/lwjgl-platform-2.9.0-natives-windows.jar"
)

fork := true

javaOptions in run := Seq("-Djava.library.path=native_lib/")

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % "2.10.1",
  "org.lwjgl.lwjgl" % "lwjgl" % "2.9.0",
  "org.lwjgl.lwjgl" % "lwjgl_util" % "2.9.0",
  "org.lwjgl.lwjgl" % "lwjgl-platform" % "2.9.0" classifier "natives-windows" classifier "natives-linux" classifier "natives-osx"
)
