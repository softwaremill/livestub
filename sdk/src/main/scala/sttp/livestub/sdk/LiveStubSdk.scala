package sttp.livestub.sdk

import sttp.client3.{Request, SttpBackend, Response => SttpResponse}
import sttp.livestub.api._
import sttp.model.Uri
import sttp.tapir.Endpoint
import sttp.tapir.client.sttp._

import scala.collection.immutable.ListSet

class LiveStubSdk[F[_]](uri: Uri)(implicit backend: SttpBackend[F, Any]) {

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
      )
    )
  }

  def when[I, E, O, S](endpoint: Endpoint[I, E, O, S]): OutgoingStubbing[F] = {
    new OutgoingStubbing(
      uri,
      RequestStubIn(
        endpoint.httpMethod.map(MethodStub.FixedMethod).getOrElse(MethodStub.Wildcard),
        endpoint.renderPathTemplate((_, _) => "*", Some((_, _) => "*"), includeAuth = false)
      )
    )
  }

  def when(requestStub: RequestStubIn): OutgoingStubbing[F] = {
    new OutgoingStubbing(uri, requestStub)
  }
}

class OutgoingStubbing[F[_]](uri: Uri, requestStub: RequestStubIn)(implicit
    backend: SttpBackend[F, Any]
) {
  def thenRespond(response: Response): F[SttpResponse[Either[Unit, StubEndpointResponse]]] = {
    SttpClientInterpreter
      .toRequestThrowDecodeFailures(LiveStubApi.setupEndpoint, Some(uri))
      .apply(
        StubEndpointRequest(
          requestStub,
          response
        )
      )
      .send(backend)
  }
}
