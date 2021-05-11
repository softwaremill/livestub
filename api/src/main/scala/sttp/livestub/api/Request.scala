package sttp.livestub.api

import sttp.model.{Method, Uri}

case class Request(
    method: Method,
    paths: List[RequestPath],
    queries: List[RequestQuery]
)

case class RequestPath(path: String)
case class RequestQuery(key: String, values: Seq[String])

object Request {
  def apply(method: Method, paths: Seq[String], queries: Seq[(String, Seq[String])]): Request = {
    new Request(
      method,
      paths.map(RequestPath).toList,
      queries.map(s => RequestQuery(s._1, s._2)).toList
    )
  }

  def apply(method: Method, path: String): Request = {
    val uri = Uri.parse(s"http://localhost/$path").right.get
    Request(method, uri.path.filter(_.nonEmpty), uri.params.toMultiSeq)
  }
}
