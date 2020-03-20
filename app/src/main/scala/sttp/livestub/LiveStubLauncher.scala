package sttp.livestub

import cats.effect._
import cats.implicits._
import com.monovore.decline.Opts
import com.monovore.decline.effect._

import scala.concurrent.ExecutionContext.Implicits.global

object LiveStubLauncher
    extends CommandIOApp(name = "com.softwaremill.sttp.livestub:livestub-app_2.13", header = "Stub everything!") {
  override def main: Opts[IO[ExitCode]] = {
    val portOpt =
      Opts.option[Int]("port", help = "http port", short = "p").withDefault(7070)

    val quietOpt = Opts.flag("quiet", help = "Whether to be quiet.", short = "q").orFalse

    (portOpt, quietOpt).mapN { (port, quiet) => new LiveStubServer(port, quiet).run }
  }
}
