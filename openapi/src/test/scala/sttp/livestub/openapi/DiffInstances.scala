package sttp.livestub.openapi

import com.softwaremill.diffx.{Derived, Diff}
import io.circe.Json
import sttp.livestub.openapi.OpenapiModels.{
  OpenapiDocument,
  OpenapiInfo,
  OpenapiParameter,
  OpenapiPath,
  OpenapiPathMethod,
  OpenapiRequestBody,
  OpenapiRequestBodyContent,
  OpenapiResponse,
  OpenapiResponseContent,
  ResponseStatusCode
}
import sttp.livestub.openapi.OpenapiSchemaType.{OpenapiSchemaObject, OpenapiSchemaSimpleType}
import sttp.model.{MediaType, Method, StatusCode}

trait DiffInstances {
  implicit lazy val methodDiff: Diff[Method] = Diff.derived[Method]
  implicit lazy val openapiSchemaSimpleTypeDiff: Diff[OpenapiSchemaSimpleType] =
    Diff.derived[OpenapiSchemaSimpleType]
  implicit lazy val jsonDiff: Diff[Json] = Diff.useEquals[Json]
  implicit lazy val openapiSchemaType: Diff[OpenapiSchemaType] = Diff.derived[OpenapiSchemaType]
  implicit lazy val openapiSchemaObjectDiff: Diff[OpenapiSchemaObject] = Diff.derived[OpenapiSchemaObject]
  implicit lazy val openapiParamTypeDiff: Diff[OpenapiParamType] = Diff.derived[OpenapiParamType]
  implicit lazy val openapiParameterDiff: Diff[OpenapiParameter] = Diff.derived[OpenapiParameter]
  implicit lazy val statusCodeDiff: Diff[StatusCode] = Diff.useEquals[StatusCode]
  implicit lazy val openapiResponseStatusCodeDiff: Diff[ResponseStatusCode] = Diff.derived[ResponseStatusCode]
  implicit lazy val mediaTypeDiff: Diff[MediaType] = Diff.derived[MediaType]
  implicit lazy val responseContentDiff: Diff[OpenapiResponseContent] = Diff.derived[OpenapiResponseContent]
  implicit lazy val responseDiff: Diff[OpenapiResponse] = Diff.derived[OpenapiResponse]
  implicit lazy val requestBodyDiff: Diff[OpenapiRequestBody] = Diff.derived[OpenapiRequestBody]
  implicit lazy val requestBodyContentDiff: Diff[OpenapiRequestBodyContent] =
    Diff.derived[OpenapiRequestBodyContent]
  implicit lazy val pathMethodDiff: Diff[OpenapiPathMethod] = Diff.derived[OpenapiPathMethod]
  implicit lazy val pathDiff: Diff[OpenapiPath] = Diff.derived[OpenapiPath]
  implicit lazy val infoDiff: Diff[OpenapiInfo] = Diff.derived[OpenapiInfo]
  implicit lazy val componentDiff: Diff[OpenapiComponent] = Diff.derived[OpenapiComponent]
  implicit lazy val documentDiff: Diff[OpenapiDocument] = Diff.derived[OpenapiDocument]
}
