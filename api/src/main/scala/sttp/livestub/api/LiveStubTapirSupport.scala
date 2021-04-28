package sttp.livestub.api

import sttp.model.{Header, StatusCode}
import sttp.tapir.SchemaType.{SInteger, SProduct, SString}
import sttp.tapir.{FieldName, Schema, SchemaType, Tapir}

trait LiveStubTapirSupport extends Tapir {
  implicit val sStatusCode: Schema[StatusCode] = Schema(SInteger)
  implicit val methodValueSchema: Schema[MethodValue] = Schema(SString)
  implicit val requestPathAndQuerySchema: Schema[RequestPathAndQuery] = Schema(SString)
  implicit val sttpHeaderSchema: Schema[Header] = Schema(
    SProduct(
      SchemaType.SObjectInfo("sttp.model.Header", List.empty),
      List(FieldName("name") -> Schema(SString), FieldName("value") -> Schema(SString))
    ),
    isOptional = false,
    description = Some("http header")
  )
}
