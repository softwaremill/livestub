package sttp.livestub.api

import scala.collection.immutable.ListSet

case class RequestQuery(queries: ListSet[QueryElement]) {
  def matches(`given`: List[QueryElement.FixedQuery]): Boolean = {
    `given`.forall { q =>
      queries.contains(q) || queries.contains(QueryElement.WildcardValueQuery(q.key)) || queries.contains(
        QueryElement.WildcardQuery
      )
    }
  }
}
object RequestQuery {
  def fromString(str: String): RequestQuery = {
    RequestQuery(
      ListSet.from(
        str
          .split('&')
          .toList
          .map(_.split('=').toList)
          .groupBy(_.head)
          .map { case (k, v) => k -> v.flatMap(_.drop(1)) }
          .map {
            case ("*", Nil)                => QueryElement.WildcardQuery
            case (k, v) if v.contains("*") => QueryElement.WildcardValueQuery(k)
            case (k, v)                    => QueryElement.FixedQuery(k, v)
          }
      )
    )
  }
}
