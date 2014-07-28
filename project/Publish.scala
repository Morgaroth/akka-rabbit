import sbt._
import sbt.Keys._


object Publish {

  lazy val settings: Seq[Setting[_]] = Seq(
    name                    := Globals.name,
    organization            := Globals.organization,
    organizationName        := Globals.organizationName,
    organizationHomepage    := Globals.organizationHomepage,
    homepage                := Globals.homepage,
    startYear               := Globals.startYear,
    organizationHomepage    := Globals.organizationHomepage,
    crossPaths              := true,
    pomExtra                := projectPomExtra,
    credentials            ++= projectRepoCredentials,
    pomIncludeRepository    := { _ => false },
    publishMavenStyle       := true,
    publishArtifact in Test := false,
    publishTo               := Globals.snapshotRepo
  )

  val projectRepoCredentials = Globals.baseCredentials ++ Seq()

  def projectPomExtra = {
      <scm>
        <url>{ Globals.scmUrl }</url>
        <connection>{ Globals.scmConnection }</connection>
      </scm>
      <developers>
        <developer>
          {Globals.pomDevelopers}
        </developer>
      </developers> ++
      {Globals.pomLicense}
  }

}
