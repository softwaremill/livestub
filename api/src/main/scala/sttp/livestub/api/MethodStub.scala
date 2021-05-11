package sttp.livestub.api

import sttp.model.Method

sealed trait MethodStub

object MethodStub {
  case class FixedMethod(method: Method) extends MethodStub
  case object Wildcard extends MethodStub
}
