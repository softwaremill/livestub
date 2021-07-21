import com.softwaremill.UpdateVersionInDocs
import sbt.Def

val http4sVersion = "0.22.0"
val circeVersion = "0.14.1"
val circeYamlVersion = "0.14.0"
val tapirVersion = "0.17.20"
val sttpClientVersion = "3.1.9"
val declineVersion = "1.4.0"

val jsonDependencies = Seq(
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-generic-extras" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirVersion
)

val loggingDependencies = Seq(
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "io.chrisdavenport" %% "log4cats-slf4j" % "1.1.1"
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
  Docker / packageName := "sttp.livestub",
  dockerUpdateLatest := true,
  Docker / version := { version.value.replace("+", "_") }
)

lazy val commonSettings: Seq[Def.Setting[_]] = commonSmlBuildSettings ++ ossPublishSettings ++ Seq(
  organization := "com.softwaremill.sttp.livestub",
  scalaVersion := "2.13.5",
  scalafmtOnCompile := false,
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/softwaremill/livestub"),
      "git@github.com:softwaremill/livestub.git"
    )
  ),
  updateDocs := Def.taskDyn {
    val files1 =
      UpdateVersionInDocs(sLog.value, organization.value, version.value, List(file("docs-sources") / "README.md"))
    Def.task {
      (docs / mdoc).toTask("").value
      files1 ++ Seq(file("generated-docs"), file("README.md"))
    }
  }.value
)

lazy val app: Project = (project in file("app"))
  .settings(commonSettings)
  .enablePlugins(DockerPlugin)
  .enablePlugins(JavaServerAppPackaging)
  .settings(dockerSettings)
  .settings(
    name := "livestub-app",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client3" %% "core" % sttpClientVersion,
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-blaze-server" % http4sVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion,
      "com.monovore" %% "decline" % declineVersion,
      "com.monovore" %% "decline-effect" % declineVersion,
      "com.softwaremill.common" %% "tagging" % "2.3.1",
      "org.typelevel" %% "cats-core" % "2.6.1",
      "org.scalatest" %% "scalatest" % "3.2.9" % Test,
      "com.codecommit" %% "cats-effect-testing-scalatest" % "0.5.4" % Test
    ) ++ loggingDependencies ++ apiDocsDependencies
  )
  .dependsOn(api, openapi)

lazy val api: Project = (project in file("api"))
  .settings(commonSettings)
  .settings(
    name := "livestub-api",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-core" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-cats" % tapirVersion,
      "org.scalatest" %% "scalatest" % "3.2.3" % Test
    ) ++ jsonDependencies
  )

lazy val sdk: Project = (project in file("sdk"))
  .settings(commonSettings)
  .settings(
    name := "livestub-sdk",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client3" %% "core" % sttpClientVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-sttp-client" % tapirVersion,
      "org.scalatest" %% "scalatest" % "3.2.3" % Test,
      "org.typelevel" %% "cats-effect" % "2.5.1",
      "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats" % sttpClientVersion % Test,
      "com.codecommit" %% "cats-effect-testing-scalatest" % "0.5.4" % Test
    )
  )
  .dependsOn(api, app % Test)

val compileDocumentation: TaskKey[Unit] = taskKey[Unit]("Compiles documentation throwing away its output")
compileDocumentation := {
  (docs / mdoc).toTask(" --out target/generated-doc").value
}

lazy val docs = project
  .in(file("generated-docs")) // important: it must not be docs/
  .settings(commonSettings)
  .enablePlugins(MdocPlugin)
  .settings(
    publishArtifact := false,
    name := "docs",
    mdocIn := file("docs-sources"),
    moduleName := "livestub-docs",
    mdocVariables := Map(
      "VERSION" -> version.value
    ),
    libraryDependencies += "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats" % sttpClientVersion,
    mdocOut := file(".")
  )
  .dependsOn(sdk, app)

lazy val openapi = project
  .in(file("openapi"))
  .settings(commonSettings)
  .settings(
    name := "openapi",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-yaml" % circeYamlVersion,
      "com.softwaremill.diffx" %% "diffx-scalatest" % "0.5.3" % Test,
      "org.scalatest" %% "scalatest" % "3.2.9" % Test
    ) ++ jsonDependencies
  )

lazy val rootProject = (project in file("."))
  .settings(commonSettings)
  .settings(publishArtifact := false, name := "livestub")
  .aggregate(app, api, sdk, docs, openapi)
