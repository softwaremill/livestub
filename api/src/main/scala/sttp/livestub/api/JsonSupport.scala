package sttp.livestub.api
import io.circe.Decoder.Result
import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.generic.extras.{AutoDerivation, Configuration}
import sttp.model.{Header, Method, StatusCode}

trait JsonSupport extends AutoDerivation {
  implicit val config: Configuration = Configuration.default.withDefaults

  implicit val statusCodeEncoder: Encoder[StatusCode] =
    Encoder.encodeInt.contramap(_.code)
  implicit val statusCodeDecoder: Decoder[StatusCode] =
    Decoder.decodeInt.map(StatusCode.unsafeApply)

  implicit val methodValueEncoder: Encoder[MethodStub] = Encoder.encodeString.contramap {
    case MethodStub.FixedMethod(method) => method.method
    case MethodStub.Wildcard            => "*"
  }
  implicit val methodValueDecoder: Decoder[MethodStub] = Decoder.decodeString.map {
    case "*"   => MethodStub.Wildcard
    case other => MethodStub.FixedMethod(Method.unsafeApply(other))
  }

  implicit val requestPathAndQueryEncoder: Encoder[RequestPathAndQuery] = {
    def pathElementToString(p: PathElement): String =
      p match {
        case PathElement.Fixed(path)   => path
        case PathElement.Wildcard      => "*"
        case PathElement.MultiWildcard => "**"
      }
    def queryElementToString(q: QueryElement): String =
      q match {
        case QueryElement.FixedQuery(key, values, _) =>
          if (values.nonEmpty) values.map(v => s"$key=$v").mkString("&") else key
        case QueryElement.WildcardValueQuery(key, _) => s"$key=*"
        case QueryElement.WildcardQuery              => "*"
      }
    Encoder.encodeString.contramap(r =>
      List(
        r.paths.map(pathElementToString).mkString("/"),
        r.queries.map(queryElementToString).mkString("&")
      )
        .mkString("?")
    )
  }
  implicit val requestPathAndQueryDecoder: Decoder[RequestPathAndQuery] =
    Decoder.decodeString.map(RequestPathAndQuery.fromString)
  implicit val headerEncoder: Encoder[Header] = (a: Header) => {
    Json.obj("name" -> Json.fromString(a.name), "value" -> Json.fromString(a.value))
  }
  implicit val headerDecoder: Decoder[Header] = (c: HCursor) => {
    for {
      name <- c.downField("name").as[String]
      value <- c.downField("value").as[String]
    } yield Header.unsafeApply(name, value)
  }
}
