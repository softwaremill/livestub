package sttp.livestub.api

import cats.data.NonEmptyList
import io.circe.{Encoder, Json}
import sttp.model._
import sttp.tapir.Codec.JsonCodec
import sttp.tapir.Endpoint
import sttp.tapir.generic.auto._
import sttp.tapir.integ.cats.TapirCodecCats
import sttp.tapir.json.circe._

import java.util.UUID
import scala.collection.immutable.ListSet

object LiveStubApi extends LiveStubTapirSupport with JsonSupport with TapirCodecCats {
  implicitly[Encoder[Header]]
//  implicitly[JsonCodec[Header]] //TODO try me!

//  val setupEndpoint: Endpoint[StubEndpointRequest, Unit, StubEndpointResponse, Any] =
//    endpoint.post
//      .in("__set")
//      .in(jsonBody[StubEndpointRequest])
//      .out(jsonBody[StubEndpointResponse])
//
//  val setupManyEndpoint: Endpoint[StubManyEndpointRequest, Unit, StubEndpointResponse, Any] =
//    endpoint.post
//      .in("__set_many")
//      .in(jsonBody[StubManyEndpointRequest])
//      .out(jsonBody[StubEndpointResponse])
//
//  val catchEndpoint: Endpoint[Request, (StatusCode, String), (StatusCode, Option[Json], List[Header]), Any] =
//    endpoint
//      .in(
//        (extractFromRequest(_.method) and paths and queryParams)
//          .map(s => Request(s._1, s._2, s._3.toMultiSeq))(r =>
//            (r.method, r.paths.map(_.path), QueryParams.fromMultiSeq(r.queries.map(q => q.key -> q.values)))
//          )
//      )
//      .out(statusCode and jsonBody[Option[Json]] and headers)
//      .errorOut(statusCode and stringBody)
//
//  val clearEndpoint: Endpoint[Unit, Unit, Unit, Any] = endpoint.post.in("__clear")
//
//  val deleteEndpoint: Endpoint[UUID, Unit, Unit, Any] = endpoint.delete
//    .in("__delete")
//    .in(path[UUID]("stubId"))
//
//  val routesEndpoint: Endpoint[Unit, Unit, StubbedRoutesResponse, Any] =
//    endpoint.get.in("__routes").out(jsonBody[StubbedRoutesResponse])
}

case class StubbedRoutesResponse(routes: List[StubEndpointResponse])
case class RequestStubOut(
    id: UUID,
    methodStub: MethodStub,
    pathStub: List[PathElement],
    queryStub: ListSet[QueryElement]
)

case class StubEndpointRequest(`when`: RequestStubIn, `then`: Response)
case class StubManyEndpointRequest(`when`: RequestStubIn, `then`: NonEmptyList[Response])
case class StubEndpointResponse(`when`: RequestStubOut, `then`: NonEmptyList[Response])
