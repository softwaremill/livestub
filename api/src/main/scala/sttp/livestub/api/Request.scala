package sttp.livestub.api

import sttp.model.{Method, Uri}

case class Request(
    method: MethodValue.FixedMethod,
    paths: List[PathElement.Fixed],
    queries: List[QueryElement.FixedQuery]
)
object Request {
  def apply(method: Method, paths: Seq[String], queries: Seq[(String, Seq[String])]): Request = {
    new Request(
      MethodValue.FixedMethod(method),
      paths.map(PathElement.Fixed).toList,
      queries.map(s => QueryElement.FixedQuery(s._1, s._2)).toList
    )
  }

  def apply(method: Method, path: String): Request = {
    val uri = Uri.parse(s"http://localhost/$path").right.get
    Request(method, uri.path.filter(_.nonEmpty), uri.multiParams.toMultiSeq)
  }
}
