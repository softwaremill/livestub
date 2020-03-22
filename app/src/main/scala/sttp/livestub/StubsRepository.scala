package sttp.livestub

import cats.data.OptionT
import cats.effect.IO
import cats.implicits._
import sttp.livestub.api._

case class StubsRepository(methods: IoMap[MethodValue, Response], paths: IoMap[PathElement, StubsRepository]) {
  def put(request: RequestStub, response: Response): IO[Unit] = {
    val pathParts = request.path.paths
    pathParts match {
      case head :: next =>
        val nextPart = request.copy(path = request.path.copy(paths = next))
        paths
          .getOrPut(head, StubsRepository())
          .flatMap(_.put(nextPart, response))
      case Nil => methods.put(request.method, response)
    }
  }

  def get(request: Request): IO[Option[Response]] = {
    val path = request.path.split("/").toList.filter(_.nonEmpty)
    path match {
      case head :: next =>
        directPath(request, head, next)
          .orElse(wildcardPath(request, next))
          .orElse(multiWildcardPath(request))
          .value
      case Nil =>
        OptionT(methods.get(MethodValue.FixedMethod(request.method))).orElseF(methods.get(MethodValue.Wildcard)).value
    }
  }

  private def directPath(request: Request, head: String, next: List[String]) = {
    OptionT(paths.get(PathElement.Fixed(head)))
      .flatMapF(_.get(request.copy(path = next.mkString("/"))))
  }

  private def wildcardPath(request: Request, next: List[String]) = {
    OptionT(paths.get(PathElement.Wildcard)).flatMapF(_.get(request.copy(path = next.mkString("/"))))
  }

  private def multiWildcardPath(request: Request) = {
    OptionT(paths.get(PathElement.MultiWildcard)).flatMapF(_.get(request.copy(path = "")))
  }

  def clear(): IO[Unit] = {
    methods.clear() >> paths.clear()
  }
}

object StubsRepository {
  def apply(): StubsRepository = new StubsRepository(new IoMap(), new IoMap())
}
