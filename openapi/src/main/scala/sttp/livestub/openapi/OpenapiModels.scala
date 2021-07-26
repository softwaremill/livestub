package sttp.livestub.openapi

import sttp.model.{MediaType, Method, StatusCode}

import scala.util.Try

// https://swagger.io/specification/
object OpenapiModels {

  case class OpenapiDocument(
      openapi: String,
      // not used so not parsed; servers, contact, license, termsOfService
      info: OpenapiInfo,
      paths: List[OpenapiPath],
      components: OpenapiComponent
  )

  case class OpenapiInfo(
      // not used so not parsed; description
      title: String,
      version: String
  )

  case class OpenapiPath(
      url: String,
      methods: List[OpenapiPathMethod]
  )

  case class OpenapiPathMethod(
      methodType: Method,
      parameters: List[OpenapiParameter],
      responses: List[OpenapiResponse],
      requestBody: Option[OpenapiRequestBody],
      summary: Option[String] = None
  )

  case class OpenapiParameter(
      name: String,
      in: OpenapiParamType,
      required: Option[Boolean],
      description: Option[String],
      schema: OpenapiSchemaType
  )

  case class OpenapiResponse(
      code: ResponseStatusCode,
      description: String,
      content: List[OpenapiResponseContent]
  )

  sealed trait ResponseStatusCode
  object ResponseStatusCode {
    case class Fixed(code: StatusCode) extends ResponseStatusCode
    case object Default extends ResponseStatusCode
  }

  case class OpenapiRequestBody(
      required: Option[Boolean],
      description: Option[String],
      content: List[OpenapiRequestBodyContent]
  )

  case class OpenapiRequestBodyContent(contentType: MediaType, schema: OpenapiSchemaType)

  case class OpenapiResponseContent(
      contentType: MediaType,
      schema: OpenapiSchemaType
  )

  /////////////////////////////////////////////////////////
  // decoders
  ////////////////////////////////////////////////////////

  import io.circe._
  import io.circe.generic.semiauto._

  implicit val OpenapiParamTypeDecoder: Decoder[OpenapiParamType] = Decoder.decodeString.emap {
    case "path"   => Right(OpenapiParamType.Path)
    case "query"  => Right(OpenapiParamType.Query)
    case "header" => Right(OpenapiParamType.Header)
    case other    => Left(s"Unrecognized param type $other")
  }

  implicit val OpenapiRequestBodyContentDecoder: Decoder[List[OpenapiRequestBodyContent]] = { (c: HCursor) =>
    case class Holder(d: OpenapiSchemaType)
    implicit val InnerDecoder: Decoder[Holder] = { (c: HCursor) =>
      for {
        schema <- c.downField("schema").as[OpenapiSchemaType]
      } yield {
        Holder(schema)
      }
    }
    for {
      responses <- c.as[Map[String, Holder]]
    } yield {
      responses.map { case (ct, s) => OpenapiRequestBodyContent(MediaType.unsafeParse(ct), s.d) }.toList
    }
  }

  implicit val OpenapiResponseContentDecoder: Decoder[List[OpenapiResponseContent]] = { (c: HCursor) =>
    case class Holder(d: OpenapiSchemaType)
    implicit val InnerDecoder: Decoder[Holder] = { (c: HCursor) =>
      for {
        schema <- c.downField("schema").as[OpenapiSchemaType]
      } yield {
        Holder(schema)
      }
    }
    for {
      responses <- c.as[Map[String, Holder]]
    } yield {
      responses.map { case (ct, s) => OpenapiResponseContent(MediaType.unsafeParse(ct), s.d) }.toList
    }
  }

  implicit val OpenapiRequestBodyDecoder: Decoder[OpenapiRequestBody] = deriveDecoder[OpenapiRequestBody]

  private object IsStatusCode {
    def unapply(code: String): Option[StatusCode] =
      Try(code.toInt).map(StatusCode.unsafeApply).toOption
  }

  implicit val OpenapiResponseDecoder: Decoder[List[OpenapiResponse]] = { (c: HCursor) =>
    implicit val InnerDecoder: Decoder[(String, List[OpenapiResponseContent])] = { (c: HCursor) =>
      for {
        description <- c.downField("description").as[String]
        content <- c.downField("content").as[Option[List[OpenapiResponseContent]]]
      } yield {
        (description, content.getOrElse(List.empty))
      }
    }
    for {
      schema <- c.as[Map[String, (String, List[OpenapiResponseContent])]]
    } yield {
      schema.collect {
        case (code, (desc, content)) if code == "default" =>
          OpenapiResponse(ResponseStatusCode.Default, desc, content)
        case (IsStatusCode(code), (desc, content)) =>
          OpenapiResponse(ResponseStatusCode.Fixed(code), desc, content)
        case (code, _) => throw new IllegalArgumentException(s"Unknown response status code: $code")
      }.toList
    }
  }

  implicit val OpenapiInfoDecoder: Decoder[OpenapiInfo] = deriveDecoder[OpenapiInfo]
  implicit val OpenapiParameterDecoder: Decoder[OpenapiParameter] = deriveDecoder[OpenapiParameter]
  implicit val OpenapiPathMethodDecoder: Decoder[List[OpenapiPathMethod]] = { (c: HCursor) =>
    implicit val InnerDecoder
        : Decoder[(List[OpenapiParameter], List[OpenapiResponse], Option[OpenapiRequestBody], Option[String])] = {
      (c: HCursor) =>
        for {
          parameters <- c.downField("parameters").as[Option[List[OpenapiParameter]]]
          responses <- c.downField("responses").as[List[OpenapiResponse]]
          requestBody <- c.downField("requestBody").as[Option[OpenapiRequestBody]]
          summary <- c.downField("summary").as[Option[String]]
        } yield {
          (parameters.getOrElse(List.empty), responses, requestBody, summary)
        }
    }
    for {
      methods <- c
        .as[Map[String, (List[OpenapiParameter], List[OpenapiResponse], Option[OpenapiRequestBody], Option[String])]]
    } yield {
      methods.map { case (t, (p, r, rb, s)) =>
        OpenapiPathMethod(Method.unsafeApply(t.toUpperCase), p, r, rb, s)
      }.toList
    }
  }

  implicit val OpenapiPathDecoder: Decoder[List[OpenapiPath]] = { (c: HCursor) =>
    for {
      paths <- c.as[Map[String, List[OpenapiPathMethod]]]
    } yield {
      paths.map { case (url, ms) => OpenapiPath(url, ms) }.toList
    }
  }

  implicit val OpenapiDocumentDecoder: Decoder[OpenapiDocument] = deriveDecoder[OpenapiDocument]

}
