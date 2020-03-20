import com.softwaremill.PublishTravis.publishTravisSettings

name := "livestub"
version := "0.1.0"
scalaVersion := "2.13.1"

organization := "sttp.livestub"
scalafmtOnCompile := true
scmInfo := Some(
  ScmInfo(
    url("https://github.com/softwaremill/livestub"),
    "git@github.com:softwaremill/livestub.git"
  )
)

val http4sVersion = "0.21.0-M5"
val circeVersion = "0.12.2"
val tapirVersion = "0.12.20"
val jsonDependencies = Seq(
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-generic-extras" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-enumeratum" % tapirVersion,
  "com.beachape" %% "enumeratum" % "1.5.15",
  "com.beachape" %% "enumeratum-circe" % "1.5.21"
)

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion
) ++ jsonDependencies

lazy val rootProject = (project in file("."))
  .settings(publishTravisSettings)
  .settings(ossPublishSettings)
