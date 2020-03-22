package sttp.livestub

import cats.data.OptionT
import cats.effect.IO
import cats.implicits._
import sttp.livestub.api._

case class StubsRepositoryImpl(
    methods: IoMap[MethodValue, Response],
    paths: IoMap[PathElement, StubsRepositoryImpl],
    queries: IoMap[RequestQuery, IoMap[MethodValue, Response]]
) extends StubRepository {
  def put(request: RequestStub, response: Response): IO[Unit] = {
    val pathParts = request.url.paths
    pathParts match {
      case head :: next =>
        val nextPart = request.copy(url = request.url.copy(paths = next))
        paths
          .getOrPut(head, StubsRepositoryImpl())
          .flatMap(_.put(nextPart, response))
      case Nil =>
        queries.getOrPut(request.url.query, new IoMap()).flatMap(_.put(request.method, response))
    }
  }

  def get(request: Request): IO[Option[Response]] = {
    request.paths match {
      case head :: next =>
        directPath(request, head, next)
          .orElse(wildcardPath(request, next))
          .orElse(multiWildcardPath(request))
          .value
      case Nil =>
        OptionT(queries.findFirst { case (rq, _) => rq.matches(request.queries) })
          .flatMap(m => OptionT(m.get(request.method)).orElseF(m.get(MethodValue.Wildcard)))
          .value
    }
  }

  private def directPath(request: Request, head: PathElement.Fixed, next: List[PathElement.Fixed]) = {
    OptionT(paths.get(head))
      .flatMapF(_.get(request.copy(paths = next)))
  }

  private def wildcardPath(request: Request, next: List[PathElement.Fixed]) = {
    OptionT(paths.get(PathElement.Wildcard)).flatMapF(_.get(request.copy(paths = next)))
  }

  private def multiWildcardPath(request: Request) = {
    OptionT(paths.get(PathElement.MultiWildcard)).flatMapF(_.get(request.copy(paths = List.empty)))
  }

  def clear(): IO[Unit] = {
    methods.clear() >> paths.clear()
  }
}

object StubsRepositoryImpl {
  def apply(): StubsRepositoryImpl = new StubsRepositoryImpl(new IoMap(), new IoMap(), new IoMap())
}

trait StubRepository {
  def get(request: Request): IO[Option[Response]]
  def put(request: RequestStub, response: Response): IO[Unit]
  def clear(): IO[Unit]
}
