package sttp.livestub.api

case class PathStub(stubs: List[PathElement]) {
  def matches(paths: List[RequestPath]): MatchResult = {
    val (result, notMatched) = stubs
      .foldLeft((MatchResult.FixedMatch: MatchResult) -> paths) {
        case ((MatchResult.NotMatched, _), _) => MatchResult.NotMatched -> Nil
        case ((mr, acc), item) =>
          val (mr2, acc2) = item.matches(acc)
          mr2.combine(mr) -> acc2
      }
    if (notMatched.nonEmpty) {
      MatchResult.NotMatched
    } else {
      result
    }
  }
}
