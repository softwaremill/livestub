package sttp.livestub.sdk

import io.circe.{Encoder, Json}
import sttp.client.asynchttpclient.WebSocketHandler
import sttp.client.{Request, SttpBackend, Response => SttpResponse}
import sttp.livestub.api._
import sttp.model.{StatusCode, Uri}
import sttp.tapir.Endpoint
import sttp.tapir.client.sttp._

class LiveStubSdk[F[_]](uri: Uri)(implicit backend: SttpBackend[F, Nothing, WebSocketHandler]) {

  def when[E, O, R](sttpRequest: Request[Either[E, O], R]): OutgoingStubbing[F] = {
    new OutgoingStubbing[F](
      uri,
      RequestStub(
        MethodValue.FixedMethod(sttpRequest.method),
        RequestPath(sttpRequest.uri.path.map(PathElement.Fixed).toList)
      )
    )
  }

  def when[I, E, O, S](endpoint: Endpoint[I, E, O, S]): OutgoingStubbing[F] = {
    new OutgoingStubbing(
      uri,
      RequestStub(
        endpoint.httpMethod.map(MethodValue.FixedMethod).getOrElse(MethodValue.Wildcard),
        RequestPath.fromString(endpoint.renderPathTemplate((_, _) => "*", Some((_, _) => "*"), includeAuth = false))
      )
    )
  }
}

class OutgoingStubbing[F[_]](uri: Uri, requestStub: RequestStub)(
    implicit backend: SttpBackend[F, Nothing, WebSocketHandler]
) {
  def `then`[T: Encoder](
      statusCode: StatusCode,
      response: T
  ): F[SttpResponse[Either[Unit, StubEndpointResponse]]] = {
    `then`(statusCode, implicitly[Encoder[T]].apply(response))
  }

  def `then`(statusCode: StatusCode, json: Json): F[SttpResponse[Either[Unit, StubEndpointResponse]]] = {
    LiveStubApi.setupEndpoint
      .toSttpRequestUnsafe(uri)
      .apply(
        StubEndpointRequest(
          requestStub,
          Response(json, statusCode)
        )
      )
      .send()
  }
}
