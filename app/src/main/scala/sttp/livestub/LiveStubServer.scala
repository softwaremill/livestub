package sttp.livestub

import cats.data.NonEmptyList
import cats.effect.{Concurrent, ContextShift, ExitCode, IO, Resource, Sync, Timer}
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe.Json
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.syntax.kleisli._
import sttp.livestub.api._
import sttp.model.{Header, StatusCode}
import sttp.tapir.docs.openapi._
import sttp.tapir.openapi.Server
import sttp.tapir.openapi.circe.yaml._
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s._
import sttp.tapir.swagger.http4s.SwaggerHttp4s

import scala.concurrent.ExecutionContext

class LiveStubServer(port: Int, quiet: Boolean) {
  private implicit def unsafeLogger[F[_]: Sync] = Slf4jLogger.getLogger[F]

  private val stubbedCalls = new ListingStubRepositoryDecorator(StubsRepositoryImpl())

  def run(implicit ec: ExecutionContext, contextShift: ContextShift[IO], timer: Timer[IO]): IO[ExitCode] =
    app.use(_ => IO.never).as(ExitCode.Success)

  val setupEndpoint: ServerEndpoint[StubEndpointRequest, Unit, StubEndpointResponse, Any, IO] =
    LiveStubApi.setupEndpoint
      .serverLogic { req =>
        log(s"Got mocking request $req") >>
          stubbedCalls
            .put(req.`when`, NonEmptyList.one(req.`then`))
            .map(_ => StubEndpointResponse().asRight[Unit])
      }

  val setupManyEndpoint: ServerEndpoint[StubManyEndpointRequest, Unit, StubEndpointResponse, Any, IO] =
    LiveStubApi.setupManyEndpoint
      .serverLogic { req =>
        log(s"Got mocking request $req") >>
          stubbedCalls
            .put(req.`when`, req.`then`)
            .map(_ => StubEndpointResponse().asRight[Unit])
      }

  val catchEndpoint: ServerEndpoint[Request, (StatusCode, String), (StatusCode, Option[Json], List[Header]), Any, IO] =
    LiveStubApi.catchEndpoint
      .serverLogic { request =>
        log(s"Got request: $request") >>
          stubbedCalls
            .get(request)
            .map(response =>
              response
                .map(r =>
                  (r.statusCode, r.body, r.headers.map(h => Header(h.name, h.value))).asRight[(StatusCode, String)]
                )
                .getOrElse(
                  (StatusCode.NotFound -> "Not mocked.")
                    .asLeft[(StatusCode, Option[Json], List[Header])]
                )
            )
      }

  val routesEndpoint: ServerEndpoint[Unit, Unit, StubbedRoutesResponse, Any, IO] =
    LiveStubApi.routesEndpoint.serverLogic { _ =>
      stubbedCalls
        .getAll()
        .map(list => StubbedRoutesResponse(list.map(i => StubEndpointRequest(i._1, i._2.head))).asRight[Unit])
    }

  val clearEndpoint: ServerEndpoint[Unit, Unit, Unit, Any, IO] =
    LiveStubApi.clearEndpoint.serverLogic(_ => stubbedCalls.clear().map(_.asRight[Unit]))

  private val endpoints: List[ServerEndpoint[_, _, _, Any, IO]] =
    List(setupEndpoint, setupManyEndpoint, clearEndpoint, routesEndpoint, catchEndpoint)

  private def log(message: String) =
    if (!quiet) {
      Logger[IO].info(message)
    } else {
      IO.unit
    }

  private def app(implicit
      ec: ExecutionContext,
      contextShift: ContextShift[IO],
      timer: Timer[IO]
  ): Resource[IO, Unit] =
    BlazeServerBuilder[IO]
      .bindHttp(port, "0.0.0.0")
      .withHttpApp(
        Router[IO](
          "/__admin" -> docsRoutes.routes[IO],
          "/" -> Http4sServerInterpreter
            .toRoutes[IO](endpoints)
        ).orNotFound
      )
      .resource
      .void

  private val docsRoutes: SwaggerHttp4s = {
    val openapi = OpenAPIDocsInterpreter
      .serverEndpointsToOpenAPI(endpoints, "Trading-Offering", "1.0")
      .copy(servers = List(Server("/", None)))
    val yaml = openapi.toYaml
    new SwaggerHttp4s(yaml)
  }
}
