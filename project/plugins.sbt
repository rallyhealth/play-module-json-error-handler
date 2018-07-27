resolvers += Resolver.url(
  "Rally Plugin Releases",
  url("https://dl.bintray.com/rallyhealth/sbt-plugins")
)(Resolver.ivyStylePatterns)

addSbtPlugin("com.rallyhealth.sbt" % "sbt-git-versioning" % "1.0.0")
addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.4")

