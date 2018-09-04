import sbt._

object Dependencies {

  final val scala_2_11 = "2.11.12"
  final val scala_2_12 = "2.12.6"

  final val play_2_5 = "2.5.18"
  final val play_2_6 = "2.6.16"

  private final val guiceVersion = "4.0"
  private final val play25JsonVersion = play_2_5
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
      case `play_2_5` => play25JsonVersion
      case `play_2_6` => play26JsonVersion
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
      case `play_2_5` => scalatestPlusPlay25Version
      case `play_2_6` => scalatestPlusPlay26Version
    }
    "org.scalatest" %% "scalatest" % version
  }

}
