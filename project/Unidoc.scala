import sbt._
import sbtunidoc.Plugin.unidocSettings

object Unidoc {

  lazy val settings: Seq[Setting[_]] = unidocSettings

}
