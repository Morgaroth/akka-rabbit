import com.typesafe.sbt.pgp.PgpKeys
import sbt._
import sbt.Keys._
import sbtrelease.ReleasePlugin.releaseSettings
import sbtrelease.ReleasePlugin.ReleaseKeys._
import sbtrelease.ReleaseStateTransformations._
import sbtrelease.ReleaseStep
import sbtrelease.Utilities._
import xerial.sbt.Sonatype.sonatypeSettings

object Release {

  lazy val settings: Seq[Setting[_]] = releaseSettings ++ Seq(
    releaseProcess := Seq[ReleaseStep](
      checkMasterBranch,
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      publishSignedArtifacts,
      setNextVersion,
      commitNextVersion,
      pushChanges
    )
  ) ++ sonatypeSettings ++ Seq(
    publishTo := { if (isSnapshot.value) Globals.snapshotRepo else publishTo.value }
  )


  lazy val publishSignedArtifacts = ReleaseStep(
    action = publishSignedArtifactAction,
    check = st => {
      // getPublishTo fails if no publish repository is set up.
      val ex = st.extract
      val ref = ex.get(thisProjectRef)
      Classpaths.getPublishTo(ex.get(publishTo in Global in ref))
      st
    },
    enableCrossBuild = true
  )
  lazy val publishSignedArtifactAction = { st: State =>
    val extracted = st.extract
    val ref = extracted.get(thisProjectRef)
    extracted.runAggregated(PgpKeys.publishSigned in Global in ref, st)
  }


  lazy val checkMasterBranch = ReleaseStep(
    action = { st: State => st },
    check = isOnMasterBranchCheck
  )
  private lazy val isOnMasterBranchCheck = { st: State =>
    val extracted = st.extract
    val vcs = extracted.get(versionControlSystem).getOrElse(sys.error("Aborting release. Working directory is not a repository of a recognized VCS."))
    val branch = vcs.currentBranch
    if (branch != "master") {
      sys.error("Aborting release. Must be in master branch.")
    }
    st
  }

}
