package sttp.livestub.api

import cats.implicits.catsSyntaxOptionId
import sttp.model.{Header, StatusCode}
import sttp.tapir.SchemaType.{SProduct, SProductField, SString}
import sttp.tapir.{FieldName, Schema, Tapir}

trait LiveStubTapirSupport extends Tapir {
  implicit val sStatusCode: Schema[StatusCode] = Schema.schemaForInt.as[StatusCode]
  implicit val methodValueSchema: Schema[MethodStub] = Schema.string[MethodStub]
  implicit val requestPathAndQuerySchema: Schema[RequestPathAndQuery] = Schema.string[RequestPathAndQuery]
  implicit val sttpHeaderSchema: Schema[Header] = Schema(
    SProduct(
      List(
        SProductField(FieldName("name"), Schema(SString()), _.name.some),
        SProductField(FieldName("value"), Schema(SString()), _.value.some)
      )
    ),
    isOptional = false,
    description = Some("http header")
  )
}
