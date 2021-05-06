package sttp.livestub.openapi

sealed trait OpenapiParamType
object OpenapiParamType {
  case object Path extends OpenapiParamType
  case object Header extends OpenapiParamType
  case object Query extends OpenapiParamType
}
