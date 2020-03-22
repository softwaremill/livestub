package sttp.livestub.api

import io.circe.{Decoder, Encoder, Json}
import sttp.model._
import sttp.tapir.SchemaType.{SInteger, SString}
import sttp.tapir.json.circe._
import sttp.tapir.{Endpoint, Schema, Tapir, Validator}

import scala.collection.immutable.ListSet
import io.circe.generic.extras.{AutoDerivation, Configuration}

object LiveStubApi extends Tapir with AutoDerivation {

  implicit val config: Configuration = Configuration.default.withDefaults

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
      case QueryElement.FixedQuery(key, values) =>
        if (values.nonEmpty) values.map(v => s"$key=$v").mkString("&") else key
      case QueryElement.WildcardValueQuery(key) => s"$key=*"
      case QueryElement.WildcardQuery           => "*"
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

  val catchEndpoint: Endpoint[Request, (StatusCode, String), (StatusCode, Json, Seq[(String, String)]), Nothing] =
    endpoint
      .in(
        (extractFromRequest(_.method) and paths and queryParams)
          .map(s => Request(s._1, s._2, s._3.toMultiSeq))(r =>
            (r.method.method, r.paths.map(_.path), MultiQueryParams.fromMultiSeq(r.queries.map(q => q.key -> q.values)))
          )
      )
      .out(statusCode and jsonBody[Json] and headers)
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
    queries: List[QueryElement.FixedQuery]
)
object Request {
  def apply(method: Method, paths: Seq[String], queries: Seq[(String, Seq[String])]): Request = {
    new Request(
      MethodValue.FixedMethod(method),
      paths.map(PathElement.Fixed).toList,
      queries.map(s => QueryElement.FixedQuery(s._1, s._2)).toList
    )
  }

  def apply(method: Method, path: String): Request = {
    val uri = Uri.parse(s"http://localhost/$path").right.get
    Request(method, uri.path.filter(_.nonEmpty), uri.multiParams.toMultiSeq)
  }
}

case class Response(body: Json, statusCode: StatusCode, headers: List[ResponseHeader] = List.empty)
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
  def matches(`given`: List[QueryElement.FixedQuery]): Boolean = {
    `given`.forall { q =>
      queries.contains(q) || queries.contains(QueryElement.WildcardValueQuery(q.key)) || queries.contains(
        QueryElement.WildcardQuery
      )
    }
  }
}
object RequestQuery {
  def fromString(str: String): RequestQuery = {
    RequestQuery(
      ListSet.from(
        str
          .split('&')
          .toList
          .map(_.split('=').toList)
          .groupBy(_.head)
          .map { case (k, v) => k -> v.flatMap(_.drop(1)) }
          .map {
            case ("*", Nil)                => QueryElement.WildcardQuery
            case (k, v) if v.contains("*") => QueryElement.WildcardValueQuery(k)
            case (k, v)                    => QueryElement.FixedQuery(k, v)
          }
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

object QueryElement {
  case class FixedQuery(key: String, values: Seq[String]) extends QueryElement
  case class WildcardValueQuery(key: String) extends QueryElement
  case object WildcardQuery extends QueryElement
}

case class ResponseHeader(name: String, value: String)
