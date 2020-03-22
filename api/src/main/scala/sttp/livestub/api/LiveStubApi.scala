package sttp.livestub.api

import io.circe.generic.auto._
import io.circe.{Decoder, Encoder, Json}
import sttp.model.{Method, StatusCode}
import sttp.tapir.Codec.PlainCodec
import sttp.tapir.SchemaType.{SInteger, SString}
import sttp.tapir.json.circe._
import sttp.tapir.{Codec, Endpoint, Schema, Tapir, Validator}

import scala.collection.Seq

object LiveStubApi extends Tapir {

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

case class MockEndpointRequest(`when`: Request, `then`: Response)
case class Request(method: Method, path: String)
object Request {
  def apply(method: Method, paths: Seq[String]): Request = {
    new Request(method, paths.mkString("/"))
  }
}

case class Response(body: Json, statusCode: StatusCode)
case class MockEndpointResponse()
