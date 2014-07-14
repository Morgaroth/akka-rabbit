import sbt._

object Resolvers {
  val sunRepo              = "Sun Maven2 Repo"        at "http://download.java.net/maven/2"
  val oracleRepo           = "Oracle Maven2 Repo"     at "http://download.oracle.com/maven"
  val sonatypeGithubRepo   = "Sonatype Github Repo"   at "http://oss.sonatype.org/content/repositories/github-releases"
  val sonatypeReleaseRepo  = "Sonatype Release Repo"  at "http://oss.sonatype.org/content/repositories/releases"
  val sonatypeSnapshotRepo = "Sonatype Snapshot Repo" at "http://oss.sonatype.org/content/repositories/snapshots"
  val clojarsRepo          = "Clojars Repo"           at "http://clojars.org/repo"
  val conjarsRepo          = "Conjars Repo"           at "http://conjars.org/repo"
  val twitter4jRepo        = "Twitter 4j Repo"        at "http://twitter4j.org/maven2"
  val twitterRepo          = "Twitter Repo"           at "http://maven.twttr.com"
  val typesafeReleaseRepo  = "Typesafe Release Repo"  at "http://repo.typesafe.com/typesafe/releases/"
  val sprayRepo            = "Spray Repo"             at "http://repo.spray.io"
  val coineyReleaseRepo    = "Coiney Release Repo"    at "http://archives.coiney.com:8888/repository/release/"
  val coineySnapshotRepo   = "Coiney Snapshot Repo"   at "http://archives.coiney.com:8888/repository/snapshots/"

  val common: Seq[MavenRepository] = Seq(
    sunRepo,
    oracleRepo,
    sonatypeReleaseRepo,
    sonatypeSnapshotRepo,
    clojarsRepo,
    conjarsRepo,
    twitter4jRepo,
    twitterRepo,
    typesafeReleaseRepo,
    coineyReleaseRepo,
    coineySnapshotRepo
  )

}
