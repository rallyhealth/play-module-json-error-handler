import Dependencies._

name := "play-module-json-error-handler-root"
organizationName in ThisBuild := "Rally Health"
organization in ThisBuild := "com.rallyhealth"

scalaVersion in ThisBuild := scala_2_11
crossScalaVersions in ThisBuild := Seq(scala_2_11, scala_2_12)

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
    case `play_2_5` => "25"
    case `play_2_6` => "26"
  }
  val projectPath = "code"
  commonProject(s"play$playSuffix-module-json-error-handler", s"play$playSuffix")
    .settings(
      crossScalaVersions := {
        includePlayVersion match {
          case `play_2_5` => Seq(scala_2_11)
          case `play_2_6` => Seq(scala_2_11, scala_2_12)
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

lazy val play25 = playModuleJsonErrorHandler(play_2_5)
lazy val play26 = playModuleJsonErrorHandler(play_2_6)
