package sttp.livestub.api

import sttp.model.Header
import sttp.tapir.SchemaType.{SProduct, SString}
import sttp.tapir.{FieldName, Schema, SchemaType, Tapir}

trait LiveStubTapirSupport extends Tapir {
  implicit val sttpHeaderSchema: Schema[Header] = Schema(
    SProduct(
      SchemaType.SObjectInfo("sttp.model.Header", List.empty),
      List(FieldName("name") -> Schema(SString), FieldName("value") -> Schema(SString))
    ),
    isOptional = false,
    description = Some("http header")
  )
}
