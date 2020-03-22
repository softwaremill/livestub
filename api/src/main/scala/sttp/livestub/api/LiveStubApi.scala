package sttp.livestub.api

import io.circe.generic.auto._
import io.circe.{Decoder, Encoder, Json}
import sttp.model.{Method, MultiQueryParams, StatusCode}
import sttp.tapir.SchemaType.{SInteger, SString}
import sttp.tapir.json.circe._
import sttp.tapir.{Endpoint, Schema, Tapir, Validator}

import scala.collection.immutable.ListSet

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

  implicit val requestPathAndQuerySchema: Schema[RequestPathAndQuery] = Schema(SString)
  implicit val requestPathAndQueryValidator: Validator[RequestPathAndQuery] = Validator.pass
  implicit val requestPathAndQueryEncoder: Encoder[RequestPathAndQuery] = {
    def pathElementToString(p: PathElement): String = p match {
      case PathElement.Fixed(path)   => path
      case PathElement.Wildcard      => "*"
      case PathElement.MultiWildcard => "**"
    }
    def queryElementToString(q: QueryElement): String = q match {
      case QueryElement.FixedKeyValueQuery(key, value) => s"$key=$value"
      case QueryElement.FixedValueQuery(key)           => key
      case QueryElement.WildcardKeyValueQuery(key)     => s"$key=*"
      case QueryElement.WildcardValueQuery             => "*"
    }
    Encoder.encodeString.contramap(r =>
      List(r.paths.map(pathElementToString).mkString("/"), r.query.queries.map(queryElementToString).mkString("&"))
        .mkString("?")
    )
  }
  implicit val requestPathAndQueryDecoder: Decoder[RequestPathAndQuery] =
    Decoder.decodeString.map(RequestPathAndQuery.fromString)

  val setupEndpoint: Endpoint[StubEndpointRequest, Unit, StubEndpointResponse, Nothing] =
    endpoint.post
      .in("__set")
      .in(jsonBody[StubEndpointRequest])
      .out(jsonBody[StubEndpointResponse])

  val catchEndpoint: Endpoint[Request, (StatusCode, String), (StatusCode, Json), Nothing] =
    endpoint
      .in(
        (extractFromRequest(_.method) and paths and queryParams)
          .map(s => Request(s._1, s._2, s._3.toSeq))(r =>
            (r.method.method, r.paths.map(_.path), MultiQueryParams.fromSeq(r.queries.map(q => q.key -> q.value)))
          )
      )
      .out(statusCode and jsonBody[Json])
      .errorOut(statusCode and stringBody)

  val clearEndpoint: Endpoint[Unit, Unit, Unit, Nothing] = endpoint.post.in("__clear")

  val routesEndpoint: Endpoint[Unit, Unit, StubbedRoutesResponse, Nothing] =
    endpoint.get.in("__routes").out(jsonBody[StubbedRoutesResponse])
}

case class StubbedRoutesResponse(routes: List[StubEndpointRequest])

case class StubEndpointRequest(`when`: RequestStub, `then`: Response)
case class Request(
    method: MethodValue.FixedMethod,
    paths: List[PathElement.Fixed],
    queries: List[QueryElement.FixedKeyValueQuery]
)
object Request {
  def apply(method: Method, paths: Seq[String], queries: Seq[(String, String)]): Request = {
    new Request(
      MethodValue.FixedMethod(method),
      paths.map(PathElement.Fixed).toList,
      queries.map(s => QueryElement.FixedKeyValueQuery(s._1, s._2)).toList
    )
  }

  def apply(method: Method, path: String): Request = {
    path.split('?').toList match {
      case List(path, query) =>
        new Request(
          MethodValue.FixedMethod(method),
          path.split("/").filter(_.nonEmpty).map(PathElement.Fixed).toList,
          query.split("&").map(FixedQueryElement.fromString).toList.collect {
            case t: QueryElement.FixedKeyValueQuery => t
          }
        )
      case path :: Nil =>
        new Request(
          MethodValue.FixedMethod(method),
          path.split("/").filter(_.nonEmpty).map(PathElement.Fixed).toList,
          List.empty
        )
    }
  }
}

case class Response(body: Json, statusCode: StatusCode)
case class StubEndpointResponse()

case class RequestStub(method: MethodValue, url: RequestPathAndQuery)
object RequestStub {
  def apply(method: MethodValue, uri: String): RequestStub = {
    new RequestStub(method, RequestPathAndQuery.fromString(uri))
  }
}

case class RequestPathAndQuery(paths: List[PathElement], query: RequestQuery)
object RequestPathAndQuery {
  def fromString(str: String): RequestPathAndQuery = {
    str.split('?').toList match {
      case List(path, query) =>
        RequestPathAndQuery(
          path.split("/").toList.filter(_.nonEmpty).map(PathElement.fromString),
          RequestQuery.fromString(query)
        )
      case path :: Nil =>
        RequestPathAndQuery(
          path.split("/").toList.filter(_.nonEmpty).map(PathElement.fromString),
          RequestQuery(ListSet.empty)
        )
    }
  }
}

case class RequestQuery(queries: ListSet[QueryElement]) {
  def matches(`given`: List[FixedQueryElement]): Boolean = {
    `given`.forall {
      case q @ QueryElement.FixedKeyValueQuery(key, _) =>
        queries.contains(q) || queries.contains(QueryElement.WildcardKeyValueQuery(key))
      case q @ QueryElement.FixedValueQuery(value) =>
        queries.contains(q) || queries.contains(QueryElement.WildcardValueQuery)
    }
  }
}
object RequestQuery {
  def fromString(str: String): RequestQuery = {
    RequestQuery(
      ListSet.from(
        str
          .split("&")
          .map(QueryElement.fromString)
          .toList
      )
    )
  }
}

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

sealed trait QueryElement

sealed trait FixedQueryElement extends QueryElement

object FixedQueryElement {
  def fromString(str: String): FixedQueryElement = {
    str.split("=").toList match {
      case List(key, value) => QueryElement.FixedKeyValueQuery(key, value)
      case value :: Nil     => QueryElement.FixedValueQuery(value)
      case other            => throw new IllegalArgumentException(s"Invalid query: $other")
    }
  }
}

object QueryElement {
  case class FixedKeyValueQuery(key: String, value: String) extends FixedQueryElement
  case class FixedValueQuery(value: String) extends FixedQueryElement
  case class WildcardKeyValueQuery(key: String) extends QueryElement
  case object WildcardValueQuery extends QueryElement

  def fromString(str: String): QueryElement = {
    str.split("=").toList match {
      case List(key, "*")   => QueryElement.WildcardKeyValueQuery(key)
      case List(key, value) => QueryElement.FixedKeyValueQuery(key, value)
      case "*" :: Nil       => QueryElement.WildcardValueQuery
      case value :: Nil     => QueryElement.FixedValueQuery(value)
      case other            => throw new IllegalArgumentException(s"Invalid query: $other")
    }
  }
}
