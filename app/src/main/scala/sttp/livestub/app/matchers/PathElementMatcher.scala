package sttp.livestub.app.matchers

import sttp.livestub.api.{PathElement, RequestPath}

private[app] object PathElementMatcher {
  def matches(paths: List[RequestPath], stubs: List[PathElement]): MatchResult = {
    val (result, notMatched) = stubs
      .foldLeft((MatchResult.FixedMatch: MatchResult) -> paths) {
        case ((MatchResult.NotMatched, _), _) => MatchResult.NotMatched -> Nil
        case ((mr, acc), item) =>
          val (mr2, acc2) = matchesSingleStub(item, acc)
          mr2.combine(mr) -> acc2
      }
    if (notMatched.nonEmpty) {
      MatchResult.NotMatched
    } else {
      result
    }
  }

  private def matchesSingleStub(stub: PathElement, requestPath: List[RequestPath]): (MatchResult, List[RequestPath]) = {
    stub match {
      case PathElement.Fixed(path) =>
        requestPath match {
          case head :: tail if head.path == path => MatchResult.FixedMatch -> tail
          case other                             => MatchResult.NotMatched -> other
        }
      case PathElement.Wildcard =>
        requestPath match {
          case _ :: tail => MatchResult.WildcardMatch -> tail
          case other     => MatchResult.NotMatched -> other
        }
      case PathElement.MultiWildcard =>
        MatchResult.MultiWildcardMatch -> Nil
    }
  }
}
