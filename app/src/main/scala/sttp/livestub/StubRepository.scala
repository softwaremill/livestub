package sttp.livestub

import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.concurrent.Ref
import sttp.livestub.api._

import java.util.concurrent.atomic.AtomicLong

case class StubRepository(
    endpoints: IoMap[RequestStub, ResponseRotator]
) {
  def put(request: RequestStub, responses: NonEmptyList[Response]): IO[Unit] = {
    endpoints.put(
      RequestStub(
        request.method,
        RequestPathAndQuery(PathStub(request.url.pathStub.stubs), request.url.queryStub)
      ),
      new ResponseRotator(responses)
    )
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

  def getAll: IO[List[(RequestStub, NonEmptyList[Response])]] =
    endpoints.getAll.map(_.view.mapValues(rr => rr.responses).toList)
}

class ResponseRotator(val responses: NonEmptyList[Response]) {
  private val counter = new AtomicLong(1)

  def getNext: Response = {
    responses.toList((counter.incrementAndGet() % responses.size).toInt)
  }
}

object StubRepository {
  def apply(): IO[StubRepository] = Ref
    .of[IO, Map[RequestStub, ResponseRotator]](Map())
    .map(new IoMap(_))
    .map(StubRepository.apply)
}
