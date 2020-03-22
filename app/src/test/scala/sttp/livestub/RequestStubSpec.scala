package sttp.livestub

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.livestub.api.{
  MethodValue,
  PathElement,
  QueryElement,
  RequestPathAndQuery,
  RequestQuery,
  RequestStub,
  StubEndpointRequest
}

import scala.collection.immutable.ListSet
import io.circe.generic.auto._
import sttp.livestub.api.LiveStubApi._

class RequestStubSpec extends AnyFlatSpec with Matchers {
  it should "parse path" in {
    RequestStub(MethodValue.Wildcard, "/admin/status").url shouldBe RequestPathAndQuery(
      List(PathElement.Fixed("admin"), PathElement.Fixed("status")),
      RequestQuery(ListSet.empty)
    )
  }

  it should "parse path with query" in {
    RequestStub(MethodValue.Wildcard, "/admin/status?filter=true").url.query shouldBe RequestQuery(
      ListSet.from(List(QueryElement.FixedKeyValueQuery("filter", "true")))
    )
  }

  it should "parse" in {
    import io.circe.parser._

    println(decode[MethodValue](""""GET""""))
    println(
      decode[StubEndpointRequest](
        """{"when":{"method":"GET", "url":"dogs/3/status?name=asd"}, "then": {"statusCode":200, "body":{"status": "unhappy"} }}"""
      )
    )
  }
}
