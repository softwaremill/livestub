package sttp.livestub.sdk

import sttp.client.{Request, SttpBackend, Response => SttpResponse}
import sttp.livestub.api._
import sttp.model.Uri
import sttp.tapir.Endpoint
import sttp.tapir.client.sttp._

import scala.collection.immutable.ListSet

class LiveStubSdk[F[_], -SS, -WS_HANDLER[_]](uri: Uri)(implicit backend: SttpBackend[F, SS, WS_HANDLER]) {

  def when[E, O, R](sttpRequest: Request[Either[E, O], R]): OutgoingStubbing[F, SS, WS_HANDLER] = {
    val req = Request(sttpRequest.method, sttpRequest.uri.path, sttpRequest.uri.params.toMultiSeq)
    new OutgoingStubbing(
      uri,
      RequestStub(
        req.method,
        RequestPathAndQuery(req.paths, RequestQuery(ListSet.from(req.queries)))
      )
    )
  }

  def when[I, E, O, S](endpoint: Endpoint[I, E, O, S]): OutgoingStubbing[F, SS, WS_HANDLER] = {
    new OutgoingStubbing(
      uri,
      RequestStub(
        endpoint.httpMethod.map(MethodValue.FixedMethod).getOrElse(MethodValue.Wildcard),
        endpoint.renderPathTemplate((_, _) => "*", Some((_, _) => "*"), includeAuth = false)
      )
    )
  }

  def when(requestStub: RequestStub): OutgoingStubbing[F, SS, WS_HANDLER] = {
    new OutgoingStubbing(uri, requestStub)
  }
}

class OutgoingStubbing[F[_], -SS, -WS_HANDLER[_]](uri: Uri, requestStub: RequestStub)(implicit
    backend: SttpBackend[F, SS, WS_HANDLER]
) {
  def thenRespond(response: Response): F[SttpResponse[Either[Unit, StubEndpointResponse]]] = {
    LiveStubApi.setupEndpoint
      .toSttpRequestUnsafe(uri)
      .apply(
        StubEndpointRequest(
          requestStub,
          response
        )
      )
      .send()
  }
}
