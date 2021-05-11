package sttp.livestub.app.matchers

import sttp.livestub.api.{MatchResult, QueryElement, RequestQuery}

import scala.collection.immutable.ListSet

private[app] object QueryElementMatcher {
  def matches(`given`: List[RequestQuery], stubs: ListSet[QueryElement]): MatchResult = {
    if (stubs.exists(_.isInstanceOf[QueryElement.WildcardQuery.type])) {
      MatchResult.MultiWildcardMatch
    } else {
      val qs = stubs.collect {
        case QueryElement.WildcardValueQuery(key, isRequired) => (key, (isRequired, QueryMatchType.Wildcard))
        case QueryElement.FixedQuery(key, values, isRequired) =>
          (key, (isRequired, QueryMatchType.Fixed(values.toList)))
      }.toMap
      val gs = `given`.map(q => q.key -> q.values).toMap

      val uniqueKeys = qs.keys ++ gs.keys
      uniqueKeys
        .map { key =>
          (qs.get(key), gs.get(key)) match {
            case (Some((required, QueryMatchType.Fixed(values))), requestValue) =>
              requestValue match {
                case Some(value) if values == value => MatchResult.FixedMatch
                case None if !required              => MatchResult.FixedMatch
                case _                              => MatchResult.NotMatched
              }
            case (Some((required, QueryMatchType.Wildcard)), requestValue) =>
              requestValue match {
                case Some(_)           => MatchResult.WildcardMatch
                case None if !required => MatchResult.WildcardMatch
                case _                 => MatchResult.NotMatched
              }
            case _ => MatchResult.NotMatched
          }
        }
        .foldLeft(MatchResult.FixedMatch: MatchResult)(_.combine(_))
    }
  }
}

private sealed trait QueryMatchType
private object QueryMatchType {
  case class Fixed(values: List[String]) extends QueryMatchType
  case object Wildcard extends QueryMatchType
}
