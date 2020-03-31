import Dependencies._

name := "play-module-json-error-handler-root"

ThisBuild / organizationName := "Rally Health"
ThisBuild / organization := "com.rallyhealth"

ThisBuild / licenses := Seq("MIT" -> url("https://opensource.org/licenses/MIT"))

ThisBuild / bintrayOrganization := Some("rallyhealth")
ThisBuild / bintrayRepository := "maven"

ThisBuild / resolvers += Resolver.bintrayRepo("rallyhealth", "maven")

// Disable publishing of root project
publish := {}
publishLocal := {}

def commonProject(id: String, path: String): Project = {
  Project(id, file(path))
    .settings(
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
    .enablePlugins(SemVerPlugin)
}

def playModuleJsonErrorHandler(includePlayVersion: String): Project = {
  val playSuffix = includePlayVersion match {
    case Play_2_5 => "25"
    case Play_2_6 => "26"
    case Play_2_7 => "27"
  }
  val scalaVersions = includePlayVersion match {
    case Play_2_5 => Seq(Scala_2_11)
    case Play_2_6 => Seq(Scala_2_11, Scala_2_12)
    case Play_2_7 => Seq(Scala_2_11, Scala_2_12, Scala_2_13)
  }
  val projectPath = "code"
  commonProject(s"play$playSuffix-module-json-error-handler", s"play$playSuffix")
    .settings(
      scalaVersion := scalaVersions.head,
      crossScalaVersions := scalaVersions,
      sourceDirectory := file(s"$projectPath/src").getAbsoluteFile,
      Compile / sourceDirectory := file(s"$projectPath/src/main").getAbsoluteFile,
      Test / sourceDirectory := file(s"$projectPath/src/test").getAbsoluteFile,
      libraryDependencies ++= Seq(
        Dependencies.guice,
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
