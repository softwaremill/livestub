package sttp.livestub.api

import scala.collection.immutable.ListSet

case class QueryStub(queries: ListSet[QueryElement]) {
  def matches(`given`: List[RequestQuery]): MatchResult = {
    if (queries.exists(_.isInstanceOf[QueryElement.WildcardQuery.type])) {
      MatchResult.MultiWildcardMatch
    } else {
      val qs = queries.collect {
        case QueryElement.WildcardValueQuery(key, isRequired) => key -> (isRequired, QueryMatchType.Wildcard)
        case QueryElement.FixedQuery(key, values, isRequired) =>
          key -> (isRequired, QueryMatchType.Fixed(values.toList))
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

sealed trait QueryMatchType
object QueryMatchType {
  case class Fixed(values: List[String]) extends QueryMatchType
  case object Wildcard extends QueryMatchType
}

object QueryStub {
  def fromString(str: String): QueryStub = {
    QueryStub(
      ListSet.from(
        str
          .split('&')
          .toList
          .map(_.split('=').toList)
          .groupBy(_.head)
          .map { case (k, v) => k -> v.flatMap(_.drop(1)) }
          .map {
            case ("*", Nil)                => QueryElement.WildcardQuery
            case (k, v) if v.contains("*") => QueryElement.WildcardValueQuery(k, isRequired = true)
            case (k, v)                    => QueryElement.FixedQuery(k, v, isRequired = true)
          }
      )
    )
  }
}
