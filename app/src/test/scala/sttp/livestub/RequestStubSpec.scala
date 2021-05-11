package sttp.livestub

import cats.data.NonEmptyList
import io.circe.Json
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.livestub.api.{
  MethodStub,
  PathElement,
  PathStub,
  QueryElement,
  QueryStub,
  RequestPathAndQuery,
  RequestStub,
  Response,
  StubEndpointRequest,
  StubManyEndpointRequest
}

import scala.collection.immutable.ListSet
import sttp.livestub.api.LiveStubApi._
import sttp.livestub.api.MethodStub.FixedMethod
import sttp.model.{Method, StatusCode}

class RequestStubSpec extends AnyFlatSpec with Matchers {

  it should "parse path" in {
    RequestStub(MethodStub.Wildcard, "/admin/status").url shouldBe RequestPathAndQuery(
      PathStub(List(PathElement.Fixed("admin"), PathElement.Fixed("status"))),
      QueryStub(ListSet.empty)
    )
  }

  it should "parse path with query" in {
    RequestStub(MethodStub.Wildcard, "/admin/status?filter=true").url.queryStub shouldBe QueryStub(
      ListSet.from(List(QueryElement.FixedQuery("filter", List("true"), isRequired = true)))
    )
  }

  it should "parse" in {
    import io.circe.parser._

    decode[StubEndpointRequest](
      """{"when":{"method":"GET", "url":"dogs/3/status?name=asd"}, "then": {"statusCode":200, "body":{"status": "unhappy"} }}"""
    ) shouldBe Right(
      StubEndpointRequest(
        RequestStub(FixedMethod(Method.GET), "dogs/3/status?name=asd"),
        Response(Some(Json.obj("status" -> Json.fromString("unhappy"))), StatusCode.Ok)
      )
    )
  }

  it should "parse many " in {
    import io.circe.parser._

    decode[StubManyEndpointRequest](
      """{"when":{"method":"GET", "url":"dogs/3/status?name=asd"}, "then": [{"statusCode":200, "body":{"status": "unhappy"} }]}"""
    ) shouldBe Right(
      StubManyEndpointRequest(
        RequestStub(FixedMethod(Method.GET), "dogs/3/status?name=asd"),
        NonEmptyList.one(Response(Some(Json.obj("status" -> Json.fromString("unhappy"))), StatusCode.Ok))
      )
    )
  }
}
