package sttp.livestub.app.repository

import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.concurrent.Ref
import sttp.livestub.api._
import sttp.livestub.app.matchers.{MatchResult, MethodMatcher, PathElementMatcher, QueryElementMatcher}

import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import scala.collection.immutable.ListSet

case class StubRepository(
    endpoints: IoMap[EndpointStub, ResponseRotator]
) {
  def put(request: EndpointStub, responses: NonEmptyList[Response]): IO[Unit] = {
    endpoints.put(request, new ResponseRotator(responses))
  }

  def get(request: Request): IO[Option[Response]] = {
    endpoints
      .collect { case (reqStub, response) =>
        reqStub.matches(request) -> response
      }
      .map { list =>
        val map = list.toMap
        map
          .get(MatchResult.FixedMatch)
          .orElse(map.get(MatchResult.WildcardMatch))
          .orElse(map.get(MatchResult.MultiWildcardMatch))
          .map(_.getNext)
      }
  }

  def clear(): IO[Unit] = {
    endpoints.clear()
  }

  def getAll: IO[List[(EndpointStub, NonEmptyList[Response])]] =
    endpoints.getAll.map(_.view.mapValues(rr => rr.responses).toList)

  def remove(id: UUID): IO[Unit] = {
    endpoints.remove(k => k.id == id)
  }
}

case class EndpointStub(
    id: UUID,
    methodStub: MethodStub,
    pathStub: List[PathElement],
    queryStub: ListSet[QueryElement]
) {
  def matches(request: Request): MatchResult = {
    MethodMatcher
      .matches(request.method, methodStub)
      .combine(PathElementMatcher.matches(request.paths, pathStub))
      .combine(QueryElementMatcher.matches(request.queries, queryStub))
  }
}
object EndpointStub {
  def apply(methodStub: MethodStub, pathStub: List[PathElement], queryStub: ListSet[QueryElement]): EndpointStub =
    new EndpointStub(UUID.randomUUID(), methodStub, pathStub, queryStub)
}

class ResponseRotator(val responses: NonEmptyList[Response]) {
  private val counter = new AtomicLong(1)

  def getNext: Response = {
    responses.toList((counter.incrementAndGet() % responses.size).toInt)
  }
}

object StubRepository {
  def apply(): IO[StubRepository] = Ref
    .of[IO, Map[EndpointStub, ResponseRotator]](Map())
    .map(new IoMap(_))
    .map(StubRepository.apply)
}
