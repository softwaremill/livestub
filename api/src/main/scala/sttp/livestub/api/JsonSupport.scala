package sttp.livestub.api
import io.circe.{Decoder, Encoder}
import io.circe.generic.extras.{AutoDerivation, Configuration}
import sttp.model.{Method, StatusCode}

trait JsonSupport extends AutoDerivation {
  implicit val config: Configuration = Configuration.default.withDefaults

  implicit val statusCodeEncoder: Encoder[StatusCode] =
    Encoder.encodeInt.contramap(_.code)
  implicit val statusCodeDecoder: Decoder[StatusCode] =
    Decoder.decodeInt.map(StatusCode.unsafeApply)

  implicit val methodValueEncoder: Encoder[MethodValue] = Encoder.encodeString.contramap {
    case MethodValue.FixedMethod(method) => method.method
    case MethodValue.Wildcard            => "*"
  }
  implicit val methodValueDecoder: Decoder[MethodValue] = Decoder.decodeString.map {
    case "*"   => MethodValue.Wildcard
    case other => MethodValue.FixedMethod(Method.unsafeApply(other))
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
      List(r.paths.map(pathElementToString).mkString("/"), r.query.queries.map(queryElementToString).mkString("&"))
        .mkString("?")
    )
  }
  implicit val requestPathAndQueryDecoder: Decoder[RequestPathAndQuery] =
    Decoder.decodeString.map(RequestPathAndQuery.fromString)

}
