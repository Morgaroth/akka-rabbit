import sbt._
import sbt.Keys._
import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.SbtNativePackager.NativePackagerKeys._


object Package {

  lazy val applicationSettings: Seq[Setting[_]] =
    packageArchetype.java_server ++ deploymentSettings ++ resourceSettings

  lazy val serverSettings: Seq[Setting[_]] =
    packageArchetype.java_server ++ deploymentSettings ++ resourceSettings

  lazy val linuxSettings: Seq[Setting[_]] = Seq(
    maintainer in Linux         := Globals.maintainer,
    packageSummary in Linux     := Globals.summary,
    packageDescription in Linux := Globals.description,
    daemonUser in Linux         := Globals.serviceDaemonUser,
    daemonGroup in Linux        := Globals.serviceDaemonGroup
  )

  lazy val rpmSettings: Seq[Setting[_]] = linuxSettings ++ Seq(
    rpmLicense                  := Globals.license,
    rpmVendor                   := Globals.organizationName,
    rpmPostun                   := None,
    rpmPre                      := None
  )


  lazy val resourceSettings: Seq[Setting[_]] = Seq(
    mappings in Universal <+= (packageBin in Compile, sourceDirectory) map { (_, src) =>
      val config = src / "main" / "resources" / "reference.conf"
      config -> "conf/reference.conf"
    },
    mappings in Universal <+= (packageBin in Compile, sourceDirectory) map { (_, src) =>
      val config = src / "main" / "resources" / "reference.conf"
      config -> "conf/app.conf"
    },
    mappings in Universal <+= (packageBin in Compile, sourceDirectory) map { (_, src) =>
      val logback = src / "main" / "resources" / "logback.xml"
      logback -> "conf/logback.xml"
    },
    bashScriptExtraDefines += """addJava "-Dconfig.file=${app_home}/../conf/app.conf"""",
    bashScriptExtraDefines += """addJava "-Dlogback.configurationFile=${app_home}/../conf/logback.xml""""
  )

}
