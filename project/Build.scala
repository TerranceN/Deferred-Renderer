import sbt._

import Keys._
import util.Properties
import scala.sys.process._

import java.io.File

object LWJGLProject extends Build {

  val beforeRun = TaskKey[Unit]("beforeRun")
  val jarsToExtract = SettingKey[Seq[String]]("jars-to-extract")

  lazy val root = Project("root", file("."), settings = Project.defaultSettings ++ Seq(
    jarsToExtract := Seq(),
    beforeRun := { },
    beforeRun <<= (beforeRun, jarsToExtract) map { (br, jars) =>
      jars map { jar =>
        val errbuffer = new StringBuffer();
        val outbuffer = new StringBuffer();
        val ret = sys.process.Process("jar xf " + "../" + jar, new File("native_lib/")) ! 
        //log output and err
        ProcessLogger(outbuffer append _ + "\n", outbuffer append _ + "\n");
        println(ret)
      }
    },
    run <<= (run in Compile) dependsOn beforeRun
  ))
}
