package sttp.livestub.sdk

import cats.effect.Resource
import cats.effect.kernel.MonadCancelThrow
import cats.syntax.all._
import sttp.client3.{Request, SttpBackend}
import sttp.livestub.api._
import sttp.model.Uri
import sttp.tapir.Endpoint
import sttp.tapir.client.sttp._

import scala.collection.immutable.ListSet

class LiveStubSdk[F[_]: MonadCancelThrow](uri: Uri, backend: SttpBackend[F, Any]) {

  def when[E, O, R](sttpRequest: Request[Either[E, O], R]): OutgoingStubbing[F] = {
    val req = Request(sttpRequest.method, sttpRequest.uri.path, sttpRequest.uri.params.toMultiSeq)
    new OutgoingStubbing(
      uri,
      RequestStubIn(
        MethodStub.FixedMethod(req.method),
        RequestPathAndQuery(
          req.paths.map(rp => PathElement.Fixed(rp.path)),
          ListSet.from(req.queries.map(rq => QueryElement.FixedQuery(rq.key, rq.values, isRequired = true)))
        )
      ),
      backend
    )
  }

  def when[P, I, E, O, S](endpoint: Endpoint[P, I, E, O, S]): OutgoingStubbing[F] = {
    new OutgoingStubbing(
      uri,
      RequestStubIn(
        endpoint.method.map(MethodStub.FixedMethod).getOrElse(MethodStub.Wildcard),
        endpoint.showPathTemplate((_, _) => "*", Some((_, _) => "*"), includeAuth = false)
      ),
      backend
    )
  }

  def when(requestStub: RequestStubIn): OutgoingStubbing[F] = {
    new OutgoingStubbing(uri, requestStub, backend)
  }
}

class OutgoingStubbing[F[_]: MonadCancelThrow](uri: Uri, requestStub: RequestStubIn, backend: SttpBackend[F, Any]) {
  def thenRespondR(stubResponse: Response): Resource[F, Unit] = {
    Resource.make(setupStub(stubResponse))(deleteStub).void
  }

  def thenRespond(stubResponse: Response): F[Unit] = {
    thenRespondR(stubResponse).allocated.map(_._1)
  }

  private def deleteStub(response: StubEndpointResponse) = {
    SttpClientInterpreter()
      .toRequestThrowDecodeFailures(LiveStubApi.deleteEndpoint, Some(uri))
      .apply(response.`when`.id)
      .send(backend)
      .flatMap { response =>
        response.body match {
          case Left(_) =>
            new RuntimeException(s"Error while deleting stub $requestStub")
              .raiseError[F, Unit]
          case Right(value) => value.pure[F]
        }
      }
  }

  private def setupStub(stubResponse: Response) = {
    SttpClientInterpreter()
      .toRequestThrowDecodeFailures(LiveStubApi.setupEndpoint, Some(uri))
      .apply(
        StubEndpointRequest(
          requestStub,
          stubResponse
        )
      )
      .send(backend)
      .flatMap { response =>
        response.body match {
          case Left(_) =>
            new RuntimeException(s"Error while stubbing request $requestStub with response $response")
              .raiseError[F, StubEndpointResponse]
          case Right(value) => value.pure[F]
        }
      }
  }
}
