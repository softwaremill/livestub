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
  implicit lazy val methodDiff: Derived[Diff[Method]] = Diff.derived[Method]
  implicit lazy val openapiSchemaSimpleTypeDiff: Derived[Diff[OpenapiSchemaSimpleType]] =
    Diff.derived[OpenapiSchemaSimpleType]
  implicit lazy val jsonDiff: Diff[Json] = Diff.useEquals[Json]
  implicit lazy val openapiSchemaType: Derived[Diff[OpenapiSchemaType]] = Diff.derived[OpenapiSchemaType]
  implicit lazy val openapiSchemaObjectDiff: Derived[Diff[OpenapiSchemaObject]] = Diff.derived[OpenapiSchemaObject]
  implicit lazy val openapiParamTypeDiff: Derived[Diff[OpenapiParamType]] = Diff.derived[OpenapiParamType]
  implicit lazy val openapiParameterDiff: Derived[Diff[OpenapiParameter]] = Diff.derived[OpenapiParameter]
  implicit lazy val statusCodeDiff: Diff[StatusCode] = Diff.useEquals[StatusCode]
  implicit lazy val openapiResponseStatusCodeDiff: Derived[Diff[ResponseStatusCode]] = Diff.derived[ResponseStatusCode]
  implicit lazy val mediaTypeDiff: Derived[Diff[MediaType]] = Diff.derived[MediaType]
  implicit lazy val responseContentDiff: Derived[Diff[OpenapiResponseContent]] = Diff.derived[OpenapiResponseContent]
  implicit lazy val responseDiff: Derived[Diff[OpenapiResponse]] = Diff.derived[OpenapiResponse]
  implicit lazy val requestBodyDiff: Derived[Diff[OpenapiRequestBody]] = Diff.derived[OpenapiRequestBody]
  implicit lazy val requestBodyContentDiff: Derived[Diff[OpenapiRequestBodyContent]] =
    Diff.derived[OpenapiRequestBodyContent]
  implicit lazy val pathMethodDiff: Derived[Diff[OpenapiPathMethod]] = Diff.derived[OpenapiPathMethod]
  implicit lazy val pathDiff: Derived[Diff[OpenapiPath]] = Diff.derived[OpenapiPath]
  implicit lazy val infoDiff: Derived[Diff[OpenapiInfo]] = Diff.derived[OpenapiInfo]
  implicit lazy val componentDiff: Derived[Diff[OpenapiComponent]] = Diff.derived[OpenapiComponent]
  implicit lazy val documentDiff: Derived[Diff[OpenapiDocument]] = Diff.derived[OpenapiDocument]
}
