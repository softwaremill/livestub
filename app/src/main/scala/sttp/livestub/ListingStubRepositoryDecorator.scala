package sttp.livestub
import java.util.concurrent.ConcurrentHashMap

import cats.data.NonEmptyList
import cats.effect.IO
import cats.implicits._
import sttp.livestub.api.{Request, RequestStub, Response}

import scala.jdk.CollectionConverters._

class ListingStubRepositoryDecorator(repository: StubRepository) extends StubRepository {

  private val stubs = ConcurrentHashMap.newKeySet[(RequestStub, NonEmptyList[Response])]()

  override def get(request: Request): IO[Option[Response]] = repository.get(request)

  override def put(request: RequestStub, responses: NonEmptyList[Response]): IO[Unit] = {
    repository.put(request, responses) >> IO.delay(stubs.add(request -> responses))
  }

  override def clear(): IO[Unit] = {
    IO.delay(stubs.clear()) >> repository.clear()
  }

  def getAll(): IO[List[(RequestStub, NonEmptyList[Response])]] = IO.delay(stubs.asScala.toList)

}
