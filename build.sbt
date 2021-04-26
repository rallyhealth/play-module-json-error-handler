import Dependencies._

name := "play-module-json-error-handler-root"
ThisBuild / organizationName := "Rally Health"
ThisBuild / organization := "com.rallyhealth"

scalaVersion := Scala_2_13
ThisBuild / versionScheme := Some("early-semver")
ThisBuild / licenses := Seq("MIT" -> url("https://opensource.org/licenses/MIT"))

// reload sbt when the build files change
Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / homepage := Some(url("https://github.com/rallyhealth/play-module-json-error-handler"))
ThisBuild / developers := List(
  Developer(id = "jeffmay", name = "Jeff May", email = "jeff.n.may@gmail.com", url = url("https://github.com/jeffmay")),
)

// Disable publishing of root project
publish / skip := true

// don't search for previous artifact of the root project
mimaFailOnNoPrevious := false

def commonProject(id: String, path: String): Project = {
  Project(id, file(path))
    .settings(
      // verify binary compatibility
      mimaPreviousArtifacts := Set(organization.value %% name.value % "0.6.0"),

      // disable scaladoc generation
      Compile / doc / sources := Seq.empty,
      packageDoc / publishArtifact := false,

      scalacOptions := scalacOptions.value
        .filterNot(_ == "-deprecation") ++ Seq(
        "-feature",
        "-Xfatal-warnings",
        "-deprecation:false",
      )
    )
}

def playModuleJsonErrorHandler(includePlayVersion: String): Project = {
  val playSuffix = includePlayVersion match {
    case Play_2_5 => "25"
    case Play_2_6 => "26"
    case Play_2_7 => "27"
    case Play_2_8 => "28"
  }
  val scalaVersions = includePlayVersion match {
    case Play_2_5 => Seq(Scala_2_11)
    case Play_2_6 => Seq(Scala_2_11, Scala_2_12)
    case Play_2_7 => Seq(Scala_2_11, Scala_2_12, Scala_2_13)
    case Play_2_8 => Seq(Scala_2_12, Scala_2_13)
  }
  val projectPath = "code"
  commonProject(s"play$playSuffix-module-json-error-handler", s"play$playSuffix")
    .settings(
      scalaVersion := scalaVersions.head,
      crossScalaVersions := scalaVersions,
      Compile / sourceDirectory := file(s"$projectPath/src/main").getAbsoluteFile,
      Test / sourceDirectory := file(s"$projectPath/src/test").getAbsoluteFile,
      libraryDependencies ++= Seq(
        Dependencies.guice4,
        Dependencies.play(includePlayVersion),
        Dependencies.playJson(includePlayVersion)
      ) ++ Seq(
        // Test-only dependencies
        Dependencies.playTest(includePlayVersion),
        Dependencies.playTestOps(includePlayVersion),
        Dependencies.scalatest,
      ).map(_ % Test)
    )
}

lazy val play25 = playModuleJsonErrorHandler(Play_2_5)
lazy val play26 = playModuleJsonErrorHandler(Play_2_6)
lazy val play27 = playModuleJsonErrorHandler(Play_2_7)
lazy val play28 = playModuleJsonErrorHandler(Play_2_8)
