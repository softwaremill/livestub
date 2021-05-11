package sttp.livestub.api

import scala.collection.immutable.ListSet

case class RequestPathAndQuery(paths: List[PathElement], queries: ListSet[QueryElement])
object RequestPathAndQuery {
  def fromString(str: String): RequestPathAndQuery = {
    str.split('?').toList match {
      case List(path, query) =>
        RequestPathAndQuery(
          path.split("/").toList.filter(_.nonEmpty).map(PathElement.fromString),
          queryElementsFromString(query)
        )
      case path :: Nil =>
        RequestPathAndQuery(
          path.split("/").toList.filter(_.nonEmpty).map(PathElement.fromString),
          ListSet.empty
        )
      case Nil => RequestPathAndQuery(List.empty, ListSet.empty)
      case _   => throw new IllegalArgumentException(s"Malformed url $str")
    }
  }

  private def queryElementsFromString(str: String): ListSet[QueryElement] = {
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
  }
}
