package sttp.livestub.app

import cats.effect.{ExitCode, IO, Resource}
import cats.syntax.all._
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp
import com.softwaremill.tagging.Tagger
import sttp.livestub.app.LiveStubServer.Config
import sttp.livestub.app.openapi.RandomValueGenerator.SeedTag
import sttp.livestub.openapi.YamlParser

import java.nio.file.{Files, Path}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.jdk.CollectionConverters.ListHasAsScala

object LiveStubLauncher
    extends CommandIOApp(name = "com.softwaremill.sttp.livestub:livestub-app_2.13", header = "Stub everything!") {
  override def main: Opts[IO[ExitCode]] = {
    val portOpt =
      Opts.option[Int]("port", help = "http port", short = "p").withDefault(7070)

    val quietOpt = Opts.flag("quiet", help = "Whether to be quiet.", short = "q").orFalse

    val openapiSpecOpt =
      Opts.option[Path]("openapi-spec", "path to the openapi specification", "o").orNone

    val randomDataGeneratorSeedOpt = Opts.option[Long]("seed", "random data generator seed", "s").orNone

    (portOpt, quietOpt, openapiSpecOpt, randomDataGeneratorSeedOpt).mapN { (port, quiet, openapiPath, seed) =>
      val server = openapiPath match {
        case Some(value) =>
          YamlParser.parseFile(Files.readAllLines(value).asScala.mkString("\n")) match {
            case Left(value) => Resource.eval(IO.raiseError(new RuntimeException(value)))
            case Right(value) =>
              LiveStubServer.resource(Config(port, quiet, Some(value), seed.map(_.taggedWith[SeedTag])))
          }
        case None => LiveStubServer.resource(Config(port, quiet, None, None))
      }
      server.use(_ => IO.never)
    }
  }
}
