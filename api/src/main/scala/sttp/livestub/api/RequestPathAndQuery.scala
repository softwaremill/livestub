package sttp.livestub.api

import scala.collection.immutable.ListSet

case class RequestPathAndQuery(paths: List[PathElement], query: QueryStub)
object RequestPathAndQuery {
  def fromString(str: String): RequestPathAndQuery = {
    str.split('?').toList match {
      case List(path, query) =>
        RequestPathAndQuery(
          path.split("/").toList.filter(_.nonEmpty).map(PathElement.fromString),
          QueryStub.fromString(query)
        )
      case path :: Nil =>
        RequestPathAndQuery(
          path.split("/").toList.filter(_.nonEmpty).map(PathElement.fromString),
          QueryStub(ListSet.empty)
        )
      case Nil => RequestPathAndQuery(List.empty, QueryStub(ListSet.empty))
      case _   => throw new IllegalArgumentException(s"Malformed url $str")
    }
  }
}
