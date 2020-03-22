package sttp.livestub.api

import io.circe.Json
import sttp.model._
import sttp.tapir.Endpoint
import sttp.tapir.json.circe._

object LiveStubApi extends LiveStubTapirSupport with JsonSupport {
  val setupEndpoint: Endpoint[StubEndpointRequest, Unit, StubEndpointResponse, Nothing] =
    endpoint.post
      .in("__set")
      .in(jsonBody[StubEndpointRequest])
      .out(jsonBody[StubEndpointResponse])

  val catchEndpoint
      : Endpoint[Request, (StatusCode, String), (StatusCode, Option[Json], Seq[(String, String)]), Nothing] =
    endpoint
      .in(
        (extractFromRequest(_.method) and paths and queryParams)
          .map(s => Request(s._1, s._2, s._3.toMultiSeq))(r =>
            (r.method.method, r.paths.map(_.path), MultiQueryParams.fromMultiSeq(r.queries.map(q => q.key -> q.values)))
          )
      )
      .out(statusCode and jsonBody[Option[Json]] and headers)
      .errorOut(statusCode and stringBody)

  val clearEndpoint: Endpoint[Unit, Unit, Unit, Nothing] = endpoint.post.in("__clear")

  val routesEndpoint: Endpoint[Unit, Unit, StubbedRoutesResponse, Nothing] =
    endpoint.get.in("__routes").out(jsonBody[StubbedRoutesResponse])
}

case class StubbedRoutesResponse(routes: List[StubEndpointRequest])
case class StubEndpointRequest(`when`: RequestStub, `then`: Response)
case class StubEndpointResponse()
