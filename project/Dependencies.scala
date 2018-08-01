import sbt._

object Dependencies {

  final val scala211Version = "2.11.12"
  final val scala212Version = "2.12.6"

  final val play25Version = "2.5.18"
  final val play26Version = "2.6.16"

  private final val guiceVersion = "4.0"
  private final val play25JsonVersion = play25Version
  private final val play26JsonVersion = "2.6.9"
  private final val playTestOpsVersion = "1.0.0"
  private final val scalatestVersion = "3.0.5"
  private final val scalatestPlusPlay25Version = "2.0.0"
  private final val scalatestPlusPlay26Version = "3.0.1"

  val guice: ModuleID = {
    "com.google.inject" % "guice" % guiceVersion
  }

  def play(playVersion: String): ModuleID = {
    "com.typesafe.play" %% "play" % playVersion
  }

  def playJson(playVersion: String): ModuleID = {
    val version = playVersion match {
      case `play25Version` => play25JsonVersion
      case `play26Version` => play26JsonVersion
    }
    "com.typesafe.play" %% "play-json" % version
  }

  def playTest(playVersion: String): ModuleID = {
    "com.typesafe.play" %% "play-test" % playVersion
  }

  def playTestOps(playVersion: String): ModuleID = {
    "com.rallyhealth" %% "play25-test-ops-core" % playTestOpsVersion
  }

  val scalatest: ModuleID = {
    "org.scalatest" %% "scalatest" % scalatestVersion
  }

  def scalatestPlusPlay(playVersion: String): ModuleID = {
    val version = playVersion match {
      case `play25Version` => scalatestPlusPlay25Version
      case `play26Version` => scalatestPlusPlay26Version
    }
    "org.scalatest" %% "scalatest" % version
  }

}
