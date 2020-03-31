import sbt._

object Dependencies {

  final val Scala_2_11 = "2.11.12"
  final val Scala_2_12 = "2.12.6"
  final val Scala_2_13 = "2.13.1"

  final val Play_2_5 = "2.5.18"
  final val Play_2_6 = "2.6.19"
  final val Play_2_7 = "2.7.4"

  private final val guiceVersion = "4.0"
  private final val play25JsonVersion = Play_2_5
  private final val play26JsonVersion = "2.6.9"
  private final val play27JsonVersion = Play_2_7
  private final val playTestOpsVersion = "1.3.0"
  private final val scalatestVersion = "3.1.1"

  val guice: ModuleID = {
    "com.google.inject" % "guice" % guiceVersion
  }

  def play(playVersion: String): ModuleID = {
    "com.typesafe.play" %% "play" % playVersion
  }

  def playJson(playVersion: String): ModuleID = {
    val version = playVersion match {
      case Play_2_5 => play25JsonVersion
      case Play_2_6 => play26JsonVersion
      case Play_2_7 => play27JsonVersion
    }
    "com.typesafe.play" %% "play-json" % version
  }

  def playTest(playVersion: String): ModuleID = {
    "com.typesafe.play" %% "play-test" % playVersion
  }

  def playTestOps(playVersion: String): ModuleID = {
    val playSuffix = playVersion match {
      case Play_2_5 => "25"
      case Play_2_6 => "26"
      case Play_2_7 => "27"
    }
    "com.rallyhealth" %% s"play$playSuffix-test-ops-core" % playTestOpsVersion
  }

  val scalatest: ModuleID = {
    "org.scalatest" %% "scalatest" % scalatestVersion
  }

}
