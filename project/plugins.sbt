resolvers += Resolver.url("sbt-plugins", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)

resolvers ++= Seq(
  "sonatype-releases"  at "http://oss.sonatype.org/content/repositories/releases"
)

addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.1.6")

addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.3.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8.2")

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.6.0")

addSbtPlugin("org.scoverage" %% "sbt-scoverage" % "0.98.0")

addSbtPlugin("org.scoverage" %% "sbt-coveralls" % "0.98.0")