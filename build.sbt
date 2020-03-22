import com.softwaremill.PublishTravis.publishTravisSettings

val http4sVersion = "0.21.1"
val circeVersion = "0.12.2"
val tapirVersion = "0.12.25"

val jsonDependencies = Seq(
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirVersion
)

val loggingDependencies = Seq(
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "io.chrisdavenport" %% "log4cats-slf4j" % "1.0.1"
)

val apiDocsDependencies = Seq(
  "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-http4s" % tapirVersion
)

lazy val dockerSettings = Seq(
  dockerExposedPorts := Seq(7070),
  dockerBaseImage := "openjdk:8u212-jdk-stretch",
  dockerUsername := Some("softwaremill"),
  packageName in Docker := "sttp.livestub",
  dockerUpdateLatest := true
)

lazy val commonSettings = commonSmlBuildSettings ++ ossPublishSettings ++ acyclicSettings ++ Seq(
  organization := "com.softwaremill.sttp.livestub",
  scalaVersion := "2.13.1",
  scalafmtOnCompile := true,
  libraryDependencies ++= Seq(compilerPlugin("com.softwaremill.neme" %% "neme-plugin" % "0.0.5")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/softwaremill/livestub"),
      "git@github.com:softwaremill/livestub.git"
    )
  )
)

lazy val app: Project = (project in file("app"))
  .settings(commonSettings)
  .enablePlugins(DockerPlugin)
  .enablePlugins(JavaServerAppPackaging)
  .settings(dockerSettings)
  .settings(
    name := "livestub-app",
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-blaze-server" % http4sVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion,
      "com.monovore" %% "decline" % "1.0.0",
      "org.typelevel" %% "cats-core" % "2.1.1",
      "com.monovore" %% "decline-effect" % "1.0.0",
      "org.scalatest" %% "scalatest" % "3.1.1" % Test
    ) ++ loggingDependencies ++ apiDocsDependencies
  )
  .dependsOn(api)

lazy val api: Project = (project in file("api"))
  .settings(commonSettings)
  .settings(
    name := "livestub-api",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-core" % tapirVersion
    ) ++ jsonDependencies
  )

lazy val rootProject = (project in file("."))
  .settings(commonSettings)
  .settings(publishArtifact := false, name := "livestub")
  .settings(publishTravisSettings)
  .aggregate(app, api)
