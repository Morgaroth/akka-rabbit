import sbt._
import sbtassembly.Plugin.assemblySettings
import sbtassembly.Plugin.AssemblyKeys._

object Assembly {

  lazy val settings: Seq[Setting[_]] = assemblySettings

}
