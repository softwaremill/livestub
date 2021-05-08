package sttp.livestub

import cats.data.NonEmptyList
import io.circe.Json
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.livestub.api.{
  MethodValue,
  PathElement,
  QueryElement,
  RequestPathAndQuery,
  QueryStub,
  RequestStub,
  Response,
  StubEndpointRequest,
  StubManyEndpointRequest
}

import scala.collection.immutable.ListSet
import sttp.livestub.api.LiveStubApi._
import sttp.livestub.api.MethodValue.FixedMethod
import sttp.model.{Method, StatusCode}

class RequestStubSpec extends AnyFlatSpec with Matchers {

  it should "parse path" in {
    RequestStub(MethodValue.Wildcard, "/admin/status").url shouldBe RequestPathAndQuery(
      List(PathElement.Fixed("admin"), PathElement.Fixed("status")),
      QueryStub(ListSet.empty)
    )
  }

  it should "parse path with query" in {
    RequestStub(MethodValue.Wildcard, "/admin/status?filter=true").url.query shouldBe QueryStub(
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
