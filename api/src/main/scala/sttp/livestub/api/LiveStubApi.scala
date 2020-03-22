package sttp.livestub.api

import io.circe.generic.auto._
import io.circe.{Decoder, Encoder, Json}
import sttp.model.{Method, StatusCode}
import sttp.tapir.SchemaType.{SInteger, SString}
import sttp.tapir.json.circe._
import sttp.tapir.{Endpoint, Schema, Tapir, Validator}

import scala.collection.Seq

object LiveStubApi extends Tapir {

  implicit val sStatusCode: Schema[StatusCode] = Schema(SInteger)
  implicit val statusCodeEncoder: Encoder[StatusCode] =
    Encoder.encodeInt.contramap(_.code)
  implicit val statusCodeDecoder: Decoder[StatusCode] =
    Decoder.decodeInt.map(StatusCode.unsafeApply)
  implicit val vStatusCode: Validator[StatusCode] = Validator.pass[StatusCode]

  implicit val methodValueSchema: Schema[MethodValue] = Schema(SString)
  implicit val methodValueValidator: Validator[MethodValue] = Validator.pass
  implicit val methodValueEncoder: Encoder[MethodValue] = Encoder.encodeString.contramap {
    case MethodValue.FixedMethod(method) => method.method
    case MethodValue.Wildcard            => "*"
  }
  implicit val methodValueDecoder: Decoder[MethodValue] = Decoder.decodeString.map {
    case "*"   => MethodValue.Wildcard
    case other => MethodValue.FixedMethod(Method.unsafeApply(other))
  }

  implicit val requestPathSchema: Schema[RequestPath] = Schema(SString)
  implicit val requestPathValidator: Validator[RequestPath] = Validator.pass
  implicit val requestPathEncoder: Encoder[RequestPath] = {
    def pathElementToString(p: PathElement): String = p match {
      case PathElement.Fixed(path)   => path
      case PathElement.Wildcard      => "*"
      case PathElement.MultiWildcard => "**"
    }
    Encoder.encodeString.contramap(_.paths.map(pathElementToString).mkString("/"))
  }

  implicit val requestPathDecoder: Decoder[RequestPath] = Decoder.decodeString.map { str =>
    RequestPath(str.split("/").toList.filter(_.nonEmpty).map(PathElement.fromString))
  }

  val setupEndpoint: Endpoint[MockEndpointRequest, Unit, MockEndpointResponse, Nothing] =
    endpoint.post
      .in("__set")
      .in(jsonBody[MockEndpointRequest])
      .out(jsonBody[MockEndpointResponse])

  val catchEndpoint: Endpoint[Request, (StatusCode, String), (StatusCode, Json), Nothing] =
    endpoint
      .in(
        (extractFromRequest(_.method) and paths)
          .map(s => Request.apply(s._1, s._2))(r => r.method -> r.path.split("/").toIndexedSeq)
      )
      .out(statusCode and jsonBody[Json])
      .errorOut(statusCode and stringBody)

  val clearEndpoint: Endpoint[Unit, Unit, Unit, Nothing] = endpoint.post.in("__clear")
}

case class MockEndpointRequest(`when`: RequestStub, `then`: Response)
case class Request(method: Method, path: String)
object Request {
  def apply(method: Method, paths: Seq[String]): Request = {
    new Request(method, paths.mkString("/"))
  }
}

case class Response(body: Json, statusCode: StatusCode)
case class MockEndpointResponse()

case class RequestStub(method: MethodValue, path: RequestPath)

case class RequestPath(paths: List[PathElement])

sealed trait PathElement

object PathElement {
  case class Fixed(path: String) extends PathElement
  case object Wildcard extends PathElement
  case object MultiWildcard extends PathElement

  def fromString(strPath: String): PathElement = {
    strPath match {
      case "*"   => PathElement.Wildcard
      case "**"  => PathElement.MultiWildcard
      case other => PathElement.Fixed(other)
    }
  }
}

sealed trait MethodValue

object MethodValue {
  case class FixedMethod(method: Method) extends MethodValue
  case object Wildcard extends MethodValue
}
