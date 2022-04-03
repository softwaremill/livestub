package sttp.livestub.app

import cats.data.NonEmptyList
import cats.effect.{IO, Resource}
import cats.implicits._
import io.circe.Json
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.{Router, Server}
import org.typelevel.log4cats.Logger
import sttp.livestub.api._
import sttp.livestub.app.LiveStubServer.{RichEndpointStub, RichRequestStub}
import sttp.livestub.app.openapi.RandomValueGenerator.Seed
import sttp.livestub.app.openapi.{OpenapiStubsCreator, RandomValueGenerator}
import sttp.livestub.app.repository.{EndpointStub, StubRepository}
import sttp.livestub.openapi.OpenapiModels.OpenapiDocument
import sttp.model.{Header, StatusCode}
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s._
import sttp.tapir.swagger.SwaggerUIOptions
import sttp.tapir.swagger.bundle.SwaggerInterpreter

import scala.concurrent.ExecutionContext

class LiveStubServer(port: Int, quiet: Boolean, stubbedCalls: StubRepository) extends FLogger {

  def resource(implicit
      ec: ExecutionContext
  ): Resource[IO, Server] = {
    val interpreter = Http4sServerInterpreter[IO]()
    BlazeServerBuilder[IO]
      .withExecutionContext(ec)
      .bindHttp(port, "0.0.0.0")
      .withHttpApp(
        Router[IO](
          "/__admin" -> interpreter.toRoutes(docsEndpoints),
          "/" -> interpreter.toRoutes(endpoints)
        ).orNotFound
      )
      .resource
  }

  val setupEndpoint: ServerEndpoint[Any, IO] =
    LiveStubApi.setupEndpoint
      .serverLogic { req =>
        val endpointStub = req.`when`.toEndpointStub
        val thenList = NonEmptyList.one(req.`then`)
        log(s"Got mocking request $req") >>
          stubbedCalls
            .put(endpointStub, thenList)
            .map(_ => StubEndpointResponse(endpointStub.toStubOut, thenList).asRight[Unit])
      }

  val deleteEndpoint: ServerEndpoint[Any, IO] =
    LiveStubApi.deleteEndpoint
      .serverLogic { stubId =>
        log(s"Got delete stub request with id $stubId") >>
          stubbedCalls.remove(stubId).map(_.asRight[Unit])
      }

  val setupManyEndpoint: ServerEndpoint[Any, IO] =
    LiveStubApi.setupManyEndpoint
      .serverLogic { req =>
        val endpointStub = req.`when`.toEndpointStub
        log(s"Got mocking request $req") >>
          stubbedCalls
            .put(endpointStub, req.`then`)
            .map(_ => StubEndpointResponse(endpointStub.toStubOut, req.`then`).asRight[Unit])
      }

  val catchEndpoint: ServerEndpoint[Any, IO] =
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

  val routesEndpoint: ServerEndpoint[Any, IO] =
    LiveStubApi.routesEndpoint.serverLogic { _ =>
      stubbedCalls.getAll
        .map(list =>
          StubbedRoutesResponse(list.map { case (endpointStub, responses) =>
            StubEndpointResponse(endpointStub.toStubOut, responses)
          }).asRight[Unit]
        )
    }

  val clearEndpoint: ServerEndpoint[Any, IO] =
    LiveStubApi.clearEndpoint.serverLogic(_ => stubbedCalls.clear().map(_.asRight[Unit]))

  private val endpoints: List[ServerEndpoint[Any, IO]] =
    List(setupEndpoint, setupManyEndpoint, clearEndpoint, routesEndpoint, deleteEndpoint, catchEndpoint)

  private def log(message: String) =
    if (!quiet) {
      Logger[IO].info(message)
    } else {
      IO.unit
    }

  private val docsEndpoints =
    SwaggerInterpreter(swaggerUIOptions = SwaggerUIOptions.default.copy(contextPath = List("__admin")))
      .fromServerEndpoints(endpoints, "livestub", "1.0.0")
}

object LiveStubServer extends FLogger {

  case class Config(
      port: Int,
      quiet: Boolean = false,
      openapiSpec: Option[OpenapiDocument] = None,
      seed: Option[Seed] = None
  )

  def resource(config: Config)(implicit ec: ExecutionContext): Resource[IO, Server] = {
    Resource
      .eval(create(config))
      .flatMap(_.resource)
  }

  private def create(config: Config): IO[LiveStubServer] = {
    StubRepository()
      .flatTap { repository =>
        openapiSpecToRequestResponseStub(config.openapiSpec, config.seed)
          .traverse { case (request, response) =>
            Logger[IO].info(s"Stubbing $request with response $response") >>
              repository.put(
                request.toEndpointStub,
                NonEmptyList.one(response)
              )
          }
      }
      .map(r => new LiveStubServer(config.port, config.quiet, r))
  }

  private def openapiSpecToRequestResponseStub(openapiSpec: Option[OpenapiDocument], seed: Option[Seed]) = {
    openapiSpec match {
      case Some(spec) =>
        new OpenapiStubsCreator(new RandomValueGenerator(spec.components.schemas, seed))
          .apply(spec.paths)
      case None => List.empty
    }
  }
  implicit class RichRequestStub(request: RequestStubIn) {
    def toEndpointStub: EndpointStub = {
      EndpointStub(request.method, request.url.paths, request.url.queries)
    }
  }
  implicit class RichEndpointStub(endpointStub: EndpointStub) {
    def toStubOut: RequestStubOut = {
      RequestStubOut(endpointStub.id, endpointStub.methodStub, endpointStub.pathStub, endpointStub.queryStub)
    }
  }
}
