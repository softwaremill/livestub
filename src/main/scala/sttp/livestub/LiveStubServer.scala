package sttp.livestub

import java.util.concurrent.ConcurrentHashMap

import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.implicits._
import io.circe.generic.auto._
import io.circe.{Decoder, Encoder, Json}
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.{Router, Server}
import org.http4s.syntax.kleisli._
import sttp.model.{Method, StatusCode}
import sttp.tapir.Codec.PlainCodec
import sttp.tapir.SchemaType.{SInteger, SString}
import sttp.tapir._
import sttp.tapir.json.circe._
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s._

import scala.collection._

object LiveStubServer extends IOApp with Tapir {

  implicit val cMethod: PlainCodec[Method] =
    Codec.stringPlainCodecUtf8.map(str => Method.unsafeApply(str))(_.method)

  implicit val sMethod: Schema[Method] = Schema(SString)
  implicit val sStatusCode: Schema[StatusCode] = Schema(SInteger)
  implicit val methodEncoder: Encoder[Method] =
    Encoder.encodeString.contramap(_.method)
  implicit val methodDecoder: Decoder[Method] =
    Decoder.decodeString.map(s => Method.unsafeApply(s))
  implicit val statusCodeEncoder: Encoder[StatusCode] =
    Encoder.encodeInt.contramap(_.code)
  implicit val statusCodeDecoder: Decoder[StatusCode] =
    Decoder.decodeInt.map(StatusCode.unsafeApply)
  implicit val vMethod: Validator[Method] = Validator.pass[Method]
  implicit val vStatusCode: Validator[StatusCode] = Validator.pass[StatusCode]

  private val responses = new IoMap[Request, Response]()

  override def run(args: List[String]): IO[ExitCode] =
    app.use(_ => IO.never).as(ExitCode.Success)

  val setupEndpoint: ServerEndpoint[MockEndpointRequest, Unit, MockEndpointResponse, Nothing, IO] =
    endpoint.post
      .in("__set")
      .in(jsonBody[MockEndpointRequest])
      .out(jsonBody[MockEndpointResponse])
      .serverLogic { req =>
        responses
          .put(req.`when`, req.`then`)
          .map(_ => MockEndpointResponse().asRight[Unit])
      }

  val catchEndpoint: ServerEndpoint[Request, (StatusCode, String), (StatusCode, Json), Nothing, IO] =
    endpoint
      .in(
        (extractFromRequest(_.method) and paths)
          .map(s => Request.apply(s._1, s._2))(r => r.method -> r.path.split("/"))
      )
      .out(statusCode and jsonBody[Json])
      .errorOut(statusCode and stringBody)
      .serverLogic { request =>
        responses
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

  val app: Resource[IO, Server[IO]] =
    for {
      server <- BlazeServerBuilder[IO]
        .bindHttp(7070)
        .withHttpApp(
          Router("/" -> List(setupEndpoint, catchEndpoint).toRoutes).orNotFound
        )
        .resource
    } yield server
}

case class MockEndpointRequest(`when`: Request, `then`: Response)
case class Request(method: Method, path: String)
object Request {
  def apply(method: Method, paths: Seq[String]): Request = {
    new Request(method, paths.mkString("/"))
  }
}

case class Response(body: Json, statusCode: StatusCode)
case class MockEndpointResponse()

class IoMap[K, V]() {
  private val map = new ConcurrentHashMap[K, V]()

  def put(k: K, v: V): IO[Unit] = {
    IO.delay(map.put(k, v))
  }

  def get(k: K): IO[Option[V]] = {
    IO.delay(Option(map.get(k)))
  }
}
