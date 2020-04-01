resolvers += Resolver.url(
  "Rally Plugin Releases",
  url("https://dl.bintray.com/rallyhealth/sbt-plugins")
)(Resolver.ivyStylePatterns)

addSbtPlugin("com.rallyhealth.sbt" %% "sbt-git-versioning" % "1.4.0")
addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.4")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.6.1")
addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.2.7")
