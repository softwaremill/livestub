package sttp.livestub.api

import sttp.model.Method

sealed trait MethodValue

object MethodValue {
  case class FixedMethod(method: Method) extends MethodValue
  case object Wildcard extends MethodValue
}
