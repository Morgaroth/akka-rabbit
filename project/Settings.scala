import sbt._
import sbt.Keys._
import Publish.{settings => publishSettings}
import Release.{settings => releaseSettings}

object Settings {

  lazy val basicSettings: Seq[Setting[_]] = Seq(
    scalaVersion  := Globals.scalaVersion,
    resolvers    ++= Resolvers.common,
    javacOptions  := Seq(
      "-source", Globals.jvmVersion,
      "-target", Globals.jvmVersion
    ),
    scalacOptions := Seq(
      "-encoding", "utf8",
      "-g:vars",
      "-unchecked",
      "-deprecation",
      "-Yresolve-term-conflict:package"
    ),
    fork in run := true,
    parallelExecution in Test := true
  ) ++ resourceSettings ++ publishSettings ++ releaseSettings


  lazy val resourceSettings: Seq[Setting[_]] = {
    val excludedResources = Seq("application.conf", "logback.xml")
    Seq(
      mappings in (Compile, packageBin) ~= { _.filter{x => !excludedResources.contains(x._1.getName)} }
    )
  }

}
