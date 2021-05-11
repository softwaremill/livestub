package sttp.livestub.app.matchers

import sttp.livestub.api.MethodStub
import sttp.model.Method

private[app] object MethodMatcher {
  def matches(method: Method, stub: MethodStub): MatchResult = {
    stub match {
      case MethodStub.FixedMethod(fMethod) => if (method == fMethod) MatchResult.FixedMatch else MatchResult.NotMatched
      case MethodStub.Wildcard             => MatchResult.WildcardMatch
    }
  }
}
