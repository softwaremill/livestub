package sttp.livestub.api

import scala.collection.immutable.ListSet

case class QueryStub(queries: ListSet[QueryElement]) {
  def matches(`given`: List[RequestQuery]): Boolean = {
    queries.exists(_.isInstanceOf[QueryElement.WildcardQuery.type]) || queries
      .filter(_.isRequired)
      .forall(qe => `given`.exists(rq => qe.matches(rq)))
  }
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
