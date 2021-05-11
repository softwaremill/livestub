package sttp.livestub.api

import sttp.model.Method

sealed trait MethodStub {
  def matches(method: Method): MatchResult
}

object MethodStub {
  case class FixedMethod(method: Method) extends MethodStub {
    override def matches(other: Method): MatchResult =
      if (method == other) MatchResult.FixedMatch else MatchResult.NotMatched
  }
  case object Wildcard extends MethodStub {
    override def matches(method: Method): MatchResult = MatchResult.WildcardMatch
  }
}
