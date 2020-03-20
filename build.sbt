import com.softwaremill.PublishTravis.publishTravisSettings

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
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion
    ) ++ jsonDependencies
  )

lazy val rootProject = (project in file("."))
  .settings(commonSettings)
  .settings(publishArtifact := false, name := "livestub")
  .settings(publishTravisSettings)
  .aggregate(app)
