name := "sbt-git-versioning"
organizationName in ThisBuild := "Rally Health"
organization in ThisBuild := "com.rallyhealth"

scalaVersion in ThisBuild := Dependencies.scala211Version

semVerLimit in ThisBuild := "1.0.999"
licenses in ThisBuild := Seq("MIT" -> url("http://opensource.org/licenses/MIT"))

bintrayOrganization in ThisBuild := Some("rallyhealth")
bintrayRepository in ThisBuild := "ivy-scala-libs"

// Disable publishing of root project
publish := {}
publishLocal := {}

def commonProject(id: String, path: String): Project = {
  Project(id, file(path))
    .settings(
      // disable scaladoc generation
      sources in(Compile, doc) := Seq.empty,
      publishArtifact in packageDoc := false,

      publishMavenStyle := false,

      scalacOptions := scalacOptions.value
        .filterNot(_ == "-deprecation") ++ Seq(
        "-feature",
        "-Xfatal-warnings",
        "-Xlint",
        "-Ywarn-unused-import",
        "-deprecation:false"
      )
    )
    .enablePlugins(SemVerPlugin)
}

def playModuleJsonErrorHandler(includePlayVersion: String): Project = {
  val playSuffix = includePlayVersion match {
    case Dependencies.play25Version => "25"
    case Dependencies.play26Version => "26"
  }
  val projectPath = "code"
  commonProject(s"play$playSuffix-module-json-error-handler", s"play$playSuffix")
    .settings(
      crossScalaVersions := {
        includePlayVersion match {
          case Dependencies.play25Version => Seq(Dependencies.scala211Version)
          case Dependencies.play26Version => Seq(Dependencies.scala211Version, Dependencies.scala212Version)
        }
      },
      sourceDirectory := file(s"$projectPath/src").getAbsoluteFile,
      (sourceDirectory in Compile) := file(s"$projectPath/src/main").getAbsoluteFile,
      (sourceDirectory in Test) := file(s"$projectPath/src/test").getAbsoluteFile,
      libraryDependencies ++= Seq(
        Dependencies.guice,
        Dependencies.play(includePlayVersion),
        Dependencies.playJson(includePlayVersion)
      ) ++ Seq(
        // Test-only dependencies
        Dependencies.playTest(includePlayVersion),
        Dependencies.playTestOps(includePlayVersion),
        Dependencies.scalatest,
        Dependencies.scalatestPlusPlay(includePlayVersion)
      ).map(_ % Test)
    )
}

lazy val play25 = playModuleJsonErrorHandler(Dependencies.play25Version)
lazy val play26 = playModuleJsonErrorHandler(Dependencies.play26Version)
