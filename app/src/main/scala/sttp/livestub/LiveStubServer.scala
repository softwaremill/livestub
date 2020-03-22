package sttp.livestub

import cats.effect.{ContextShift, ExitCode, IO, Resource, Sync, Timer}
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe.Json
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.syntax.kleisli._
import sttp.livestub.api._
import sttp.model.StatusCode
import sttp.tapir.docs.openapi._
import sttp.tapir.openapi.Server
import sttp.tapir.openapi.circe.yaml._
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s._
import sttp.tapir.swagger.http4s.SwaggerHttp4s

import scala.concurrent.ExecutionContext

class LiveStubServer(port: Int, quiet: Boolean) {
  private implicit def unsafeLogger[F[_]: Sync] = Slf4jLogger.getLogger[F]

  private val responses = new IoMap[Request, Response]()

  def run(implicit ec: ExecutionContext, contextShift: ContextShift[IO], timer: Timer[IO]): IO[ExitCode] =
    app.use(_ => IO.never).as(ExitCode.Success)

  val setupEndpoint: ServerEndpoint[MockEndpointRequest, Unit, MockEndpointResponse, Nothing, IO] =
    LiveStubApi.setupEndpoint
      .serverLogic { req =>
        log(s"Got mocking request $req") >>
          responses
            .put(req.`when`, req.`then`)
            .map(_ => MockEndpointResponse().asRight[Unit])
      }

  val catchEndpoint: ServerEndpoint[Request, (StatusCode, String), (StatusCode, Json), Nothing, IO] =
    LiveStubApi.catchEndpoint
      .serverLogic { request =>
        log(s"Got request: $request") >> responses
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

  val clearEndpoint: ServerEndpoint[Unit, Unit, Unit, Nothing, IO] =
    LiveStubApi.clearEndpoint.serverLogic(_ => responses.clear().map(_.asRight[Unit]))

  private def log(message: String) =
    if (!quiet) {
      Logger[IO].info(message)
    } else {
      IO.unit
    }

  private val endpoints = List(setupEndpoint, catchEndpoint, clearEndpoint)

  private def app(
      implicit ec: ExecutionContext,
      contextShift: ContextShift[IO],
      timer: Timer[IO]
  ): Resource[IO, Unit] =
    BlazeServerBuilder[IO]
      .bindHttp(port, "0.0.0.0")
      .withHttpApp(
        Router("/__admin" -> docsRoutes.routes[IO], "/" -> endpoints.toRoutes).orNotFound
      )
      .resource
      .void

  private val docsRoutes: SwaggerHttp4s = {
    val openapi =
      endpoints
        .toOpenAPI("Trading-Offering", "1.0")
        .copy(servers = List(Server("/", None)))
    val yaml = openapi.toYaml
    new SwaggerHttp4s(yaml)
  }
}
