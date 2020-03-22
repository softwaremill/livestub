package sttp.livestub

import cats.effect.{ContextShift, ExitCode, IO, Resource, Sync, Timer}
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe.Json
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.{Router, Server}
import org.http4s.syntax.kleisli._
import sttp.livestub.api._
import sttp.model.StatusCode
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s._

import scala.concurrent.ExecutionContext

class LiveStubServer(port: Int, quiet: Boolean) {
  private implicit def unsafeLogger[F[_]: Sync] = Slf4jLogger.getLogger[F]

  private val responses = new IoMap[Request, Response]()

  def run(implicit ec: ExecutionContext, contextShift: ContextShift[IO], timer: Timer[IO]): IO[ExitCode] =
    app.use(_ => IO.never).as(ExitCode.Success)

  val setupEndpoint: ServerEndpoint[MockEndpointRequest, Unit, MockEndpointResponse, Nothing, IO] =
    LiveStubApi.setupEndpoint
      .serverLogic { req =>
        responses
          .put(req.`when`, req.`then`)
          .map(_ => MockEndpointResponse().asRight[Unit])
      }

  val catchEndpoint: ServerEndpoint[Request, (StatusCode, String), (StatusCode, Json), Nothing, IO] =
    LiveStubApi.catchEndpoint
      .serverLogic { request =>
        logRequest(request) >> responses
          .get(request)
          .map(response =>
            response
              .map(r => (r.statusCode -> r.body).asRight[(StatusCode, String)])
              .getOrElse(
                (StatusCode.NotFound -> "Not mocked.")
                  .asLeft[(StatusCode, Json)]
              )
          )
      }

  private def logRequest(request: Request) =
    if (!quiet) {
      Logger[IO].info(s"Got request: $request")
    } else {
      IO.unit
    }

  def app(implicit ec: ExecutionContext, contextShift: ContextShift[IO], timer: Timer[IO]): Resource[IO, Server[IO]] =
    for {
      server <- BlazeServerBuilder[IO]
        .bindHttp(port, "0.0.0.0")
        .withHttpApp(
          Router("/" -> List(setupEndpoint, catchEndpoint).toRoutes).orNotFound
        )
        .resource
    } yield server
}
