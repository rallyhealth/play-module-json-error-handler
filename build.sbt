name := "sbt-git-versioning"
organizationName := "Rally Health"
organization := "com.rallyhealth"

scalaVersion := "2.11.12"

enablePlugins(SemVerPlugin)
semVerLimit := "1.0.999"
licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT"))

bintrayOrganization := Some("rallyhealth")
bintrayRepository := "ivy-scala-libs"

publishMavenStyle := false

scalacOptions ++= Seq("-Xfatal-warnings", "-Xlint", "-Ywarn-unused-import")

// TODO: Cross-compile play version
libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play" % "2.5.18",
  "com.typesafe.play" %% "play-json" % "2.5.18"
) ++ Seq(
  // Test-only dependencies
  "com.rallyhealth" %% "play25-test-ops-core" % "1.0.0",
  "org.scalatest" %% "scalatest" % "3.0.5",
  "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0"
).map(_ % Test)

// disable scaladoc generation
sources in(Compile, doc) := Seq.empty

publishArtifact in packageDoc := false

