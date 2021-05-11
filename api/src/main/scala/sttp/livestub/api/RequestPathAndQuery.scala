package sttp.livestub.api

import scala.collection.immutable.ListSet

case class RequestPathAndQuery(pathStub: PathStub, queryStub: QueryStub) {
  def matches(paths: List[RequestPath], query: List[RequestQuery]): MatchResult = {
    pathStub.matches(paths).combine(queryStub.matches(query))
  }
}
object RequestPathAndQuery {
  def fromString(str: String): RequestPathAndQuery = {
    str.split('?').toList match {
      case List(path, query) =>
        RequestPathAndQuery(
          PathStub(path.split("/").toList.filter(_.nonEmpty).map(PathElement.fromString)),
          QueryStub.fromString(query)
        )
      case path :: Nil =>
        RequestPathAndQuery(
          PathStub(path.split("/").toList.filter(_.nonEmpty).map(PathElement.fromString)),
          QueryStub(ListSet.empty)
        )
      case Nil => RequestPathAndQuery(PathStub(List.empty), QueryStub(ListSet.empty))
      case _   => throw new IllegalArgumentException(s"Malformed url $str")
    }
  }
}
