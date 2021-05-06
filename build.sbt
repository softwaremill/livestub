import com.softwaremill.PublishTravis
import com.softwaremill.PublishTravis.publishTravisSettings
import sbtrelease.ReleaseStateTransformations._

val http4sVersion = "0.21.22"
val circeVersion = "0.13.0"
val circeYamlVersion = "0.13.1"
val tapirVersion = "0.17.19"
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
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.3",
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
      "com.softwaremill.sttp.client3" %% "core" % sttpClientVersion,
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-blaze-server" % http4sVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion,
      "com.monovore" %% "decline" % declineVersion,
      "com.monovore" %% "decline-effect" % declineVersion,
      "com.softwaremill.common" %% "tagging" % "2.2.1",
      "org.typelevel" %% "cats-core" % "2.6.0",
      "org.scalatest" %% "scalatest" % "3.2.8" % Test
    ) ++ loggingDependencies ++ apiDocsDependencies
  )
  .dependsOn(api, openapi)

lazy val api: Project = (project in file("api"))
  .settings(commonSettings)
  .settings(
    name := "livestub-api",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-core" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-cats" % tapirVersion
    ) ++ jsonDependencies
  )

lazy val sdk: Project = (project in file("sdk"))
  .settings(commonSettings)
  .settings(
    name := "livestub-sdk",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats" % sttpClientVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-sttp-client" % tapirVersion,
      "org.scalatest" %% "scalatest" % "3.2.3" % Test
    )
  )
  .dependsOn(api)

lazy val docs = project
  .in(file("generated-docs")) // important: it must not be docs/
  .settings(commonSettings)
  .settings(publishArtifact := false, name := "docs")
  .dependsOn(sdk)
  .enablePlugins(MdocPlugin)
  .settings(
    mdocIn := file("docs-sources"),
    moduleName := "livestub-docs",
    mdocVariables := Map(
      "VERSION" -> version.value
    ),
    mdocOut := file(".")
  )

lazy val openapi = project
  .in(file("openapi"))
  .settings(commonSettings)
  .settings(
    name := "openapi",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-yaml" % circeYamlVersion,
      "com.softwaremill.diffx" %% "diffx-scalatest" % "0.4.5" % Test,
      "org.scalatest" %% "scalatest" % "3.2.8" % Test
    ) ++ jsonDependencies
  )

lazy val rootProject = (project in file("."))
  .settings(commonSettings)
  .settings(publishArtifact := false, name := "livestub")
  .settings(publishTravisSettings)
  .settings(releaseProcess := {
    if (PublishTravis.isCommitRelease.value) {
      Seq(
        checkSnapshotDependencies,
        inquireVersions,
        runClean,
        runTest,
        setReleaseVersion,
        releaseStepInputTask(docs / mdoc),
        stageChanges("README.md"),
        commitReleaseVersion,
        tagRelease,
        setNextVersion,
        commitNextVersion,
        pushChanges
      )
    } else {
      Seq(
        publishArtifacts,
        releaseStepCommand("sonatypeBundleRelease")
      )
    }
  })
  .aggregate(app, api, sdk, docs, openapi)

def stageChanges(fileName: String): ReleaseStep = { s: State =>
  val settings = Project.extract(s)
  val vcs = settings.get(releaseVcs).get
  vcs.add(fileName) !! s.log
  s
}
