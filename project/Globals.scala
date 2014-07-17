import sbt._

object Globals {
  val name                  = "akka-rabbit"
  val scalaVersion          = "2.10.4"
  val jvmVersion            = "1.7"

  val homepage              = Some(url("http://www.pjan.io"))
  val startYear             = Some(2014)
  val summary               = "An asynchronous scala client for RabbitMQ, based on Akka."
  val description           = "An asynchronous scala client for RabbitMQ, based on Akka."
  val maintainer            = "pjan <pjan@coiney.com>"
  val license               = Some("BSD")

  val organizationName      = "Coiney Inc."
  val organization          = "com.coiney"
  val organizationHomepage  = Some(url("http://coiney.com"))

  val sourceUrl             = "https://github.com/Coiney/akka-rabbit"
  val scmUrl                = "git@github.com:Coiney/akka-rabbit.git"
  val scmConnection         = "scm:git:git@github.com:Coiney/akka-rabbit.git"

  val serviceDaemonUser     = "admin"
  val serviceDaemonGroup    = "admin"

  val baseCredentials: Seq[Credentials] = Seq[Credentials](
    Credentials(Path.userHome / ".ivy2" / ".credentials_coiney_snapshots"),
    Credentials(Path.userHome / ".ivy2" / ".credentials_coiney_release")
  )

  val snapshotRepo = Some("snapshots" at "http://archives.coiney.com:8888/repository/snapshots/")

  val pomDevelopers =
    <id>pjan</id><name>pjan vandaele</name><url>http://pjan.io</url>;

  val pomLicense =
    <licenses>
      <license>
        <name>The BSD 3-Clause License</name>
        <url>http://opensource.org/licenses/BSD-3-Clause</url>
        <distribution>repo</distribution>
      </license>
    </licenses>;

}
