package sttp.livestub

import cats.data.{NonEmptyList, OptionT}
import cats.effect.IO
import sttp.livestub.api._
import cats.implicits._

case class StubsRepositoryImpl(
    paths: IoMap[PathElement, StubsRepositoryImpl],
    queries: IoMap[RequestQuery, IoMap[MethodValue, NonEmptyList[Response]]]
) extends StubRepository {
  def put(request: RequestStub, responses: NonEmptyList[Response]): IO[Unit] = {
    val pathParts = request.url.paths
    pathParts match {
      case head :: next =>
        val nextPart = request.copy(url = request.url.copy(paths = next))
        paths
          .getOrPut(head, StubsRepositoryImpl())
          .flatMap(_.put(nextPart, responses))
      case Nil =>
        queries.getOrPut(request.url.query, new IoMap()).flatMap(_.put(request.method, responses))
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
          .flatMap(m =>
            getDirectMethod(request, m)
              .orElse(getWildcardMethod(m))
          )
          .map(_.head)
          .value
    }
  }

  private def getDirectMethod(request: Request, m: IoMap[MethodValue, NonEmptyList[Response]]) = {
    OptionT(m.get(request.method)).flatTap(cycleResponses(m, _, request.method))
  }

  private def getWildcardMethod(m: IoMap[MethodValue, NonEmptyList[Response]]) = {
    OptionT(m.get(MethodValue.Wildcard)).flatTap(cycleResponses(m, _, MethodValue.Wildcard))
  }

  private def cycleResponses(
      methodsToResponses: IoMap[MethodValue, NonEmptyList[Response]],
      responses: NonEmptyList[Response],
      method: MethodValue
  ) = {
    responses match {
      case NonEmptyList(head, tHead :: tTail) =>
        OptionT.liftF(methodsToResponses.put(method, NonEmptyList.of(tHead, tTail: _*) :+ head))
      case nel @ NonEmptyList(_, Nil) => OptionT.liftF(methodsToResponses.put(MethodValue.Wildcard, nel))
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
    paths.clear() >> queries.clear()
  }
}

object StubsRepositoryImpl {
  def apply(): StubsRepositoryImpl = new StubsRepositoryImpl(new IoMap(), new IoMap())
}

trait StubRepository {
  def get(request: Request): IO[Option[Response]]
  def put(request: RequestStub, responses: NonEmptyList[Response]): IO[Unit]
  def clear(): IO[Unit]
}
