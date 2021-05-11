package sttp.livestub

import cats.data.NonEmptyList
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.all._
import io.circe.Json
import org.scalatest.OptionValues
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.livestub.api._
import sttp.model.{Method, StatusCode}

import scala.collection.immutable.ListSet

class StubRepositorySpec extends AsyncFlatSpec with AsyncIOSpec with Matchers with OptionValues {

  "StubRepositorySpec" should "return response for single fragment path" in {
    val response = Response.withJsonBody(Json.fromString("OK"), StatusCode.Ok)
    for {
      repository <- StubRepository()
      _ <- repository
        .put(RequestStub(MethodStub.FixedMethod(Method.GET), "/admin"), NonEmptyList.one(response))
      _ <- repository.get(Request(Method.GET, "/admin")).map(_.value shouldBe response)
    } yield ()
  }

  it should "return same response for single fragment path for every invocation" in {
    val response = Response.withJsonBody(Json.fromString("OK"), StatusCode.Ok)
    for {
      repository <- StubRepository()
      _ <- repository
        .put(RequestStub(MethodStub.FixedMethod(Method.GET), "/admin"), NonEmptyList.one(response))
      _ <- (1 to 5).toList.traverse { _ =>
        repository.get(Request(Method.GET, "/admin")).map(_.value shouldBe response)
      }
    } yield ()
  }

  it should "return none for not matching path" in {
    for {
      repository <- StubRepository()
      _ <- repository.get(Request(Method.GET, "/admin")).map(_ shouldBe empty)
    } yield ()
  }

  it should "return response for multi fragment path" in {
    val response = Response.withJsonBody(Json.fromString("OK"), StatusCode.Ok)
    for {
      repository <- StubRepository()
      _ <- repository
        .put(RequestStub(MethodStub.FixedMethod(Method.GET), "/admin/docs"), NonEmptyList.one(response))
      _ <- repository.get(Request(Method.GET, "/admin/docs")).map(_.value shouldBe response)
    } yield ()
  }

  it should "distinguish when path contains another subpath" in {
    val response1 = Response.withJsonBody(Json.fromString("OK"), StatusCode.Ok)
    val response2 = Response.withJsonBody(Json.fromString("Bad"), StatusCode.BadRequest)
    for {
      repository <- StubRepository()
      _ <- repository
        .put(RequestStub(MethodStub.FixedMethod(Method.GET), "/admin/docs"), NonEmptyList.one(response1))
      _ <- repository
        .put(RequestStub(MethodStub.FixedMethod(Method.GET), "/admin"), NonEmptyList.one(response2))
      _ <- repository.get(Request(Method.GET, "/admin/docs")).map(_.value shouldBe response1)
      _ <- repository.get(Request(Method.GET, "/admin")).map(_.value shouldBe response2)
    } yield ()
  }

  it should "support wildcard paths" in {
    val response = Response.withJsonBody(Json.fromString("OK"), StatusCode.Ok)
    for {
      repository <- StubRepository()
      _ <- repository
        .put(RequestStub(MethodStub.FixedMethod(Method.GET), "/*"), NonEmptyList.one(response))
      _ <- repository.get(Request(Method.GET, "/admin")).map(_.value shouldBe response)
      _ <- repository.get(Request(Method.GET, "/docs")).map(_.value shouldBe response)
    } yield ()
  }

  it should "distinguish between wildcard path and direct path" in {
    val response1 = Response.withJsonBody(Json.fromString("OK"), StatusCode.Ok)
    val response2 = Response.withJsonBody(Json.fromString("Bad"), StatusCode.BadRequest)
    for {
      repository <- StubRepository()
      _ <- repository
        .put(RequestStub(MethodStub.FixedMethod(Method.GET), "/admin/*"), NonEmptyList.one(response1))
      _ <- repository
        .put(RequestStub(MethodStub.FixedMethod(Method.GET), "/admin"), NonEmptyList.one(response2))
      _ <- repository.get(Request(Method.GET, "/admin/docs")).map(_.value shouldBe response1)
      _ <- repository.get(Request(Method.GET, "/admin")).map(_.value shouldBe response2)
    } yield ()
  }

  it should "support wildcard methods" in {
    val response = Response.withJsonBody(Json.fromString("OK"), StatusCode.Ok)
    for {
      repository <- StubRepository()
      _ <- repository
        .put(RequestStub(MethodStub.Wildcard, "/admin"), NonEmptyList.one(response))
      _ <- repository.get(Request(Method.POST, "/admin")).map(_.value shouldBe response)
      _ <- repository.get(Request(Method.GET, "/admin")).map(_.value shouldBe response)
    } yield ()
  }

  it should "only resolve wildcard path elements on a single level" in {
    val response = Response.withJsonBody(Json.fromString("OK"), StatusCode.Ok)
    for {
      repository <- StubRepository()
      _ <- repository
        .put(RequestStub(MethodStub.Wildcard, "/admin/*"), NonEmptyList.one(response))
      _ <- repository.get(Request(Method.GET, "/admin/docs")).map(_.value shouldBe response)
      _ <- repository.get(Request(Method.GET, "/admin/docs/status")).map(_ shouldBe empty)
    } yield ()
  }

  it should "resolve multi wildcards globally" in {
    val response = Response.withJsonBody(Json.fromString("OK"), StatusCode.Ok)
    for {
      repository <- StubRepository()
      _ <- repository
        .put(RequestStub(MethodStub.Wildcard, "/admin/**"), NonEmptyList.one(response))
      _ <- repository.get(Request(Method.POST, "/admin/docs")).map(_.value shouldBe response)
      _ <- repository.get(Request(Method.GET, "/admin/docs/status")).map(_.value shouldBe response)
    } yield ()
  }

  it should "fixed match should have precedence over wildcard match" in {
    val response1 = Response.withJsonBody(Json.fromString("OK"), StatusCode.Ok)
    val response2 = Response.withJsonBody(Json.fromString("Bad"), StatusCode.BadRequest)
    for {
      repository <- StubRepository()
      _ <- repository
        .put(RequestStub(MethodStub.FixedMethod(Method.GET), "/user/**"), NonEmptyList.one(response1))
      _ <- repository
        .put(RequestStub(MethodStub.FixedMethod(Method.GET), "/user/list/1/status"), NonEmptyList.one(response2))
      _ <- repository.get(Request(Method.GET, "/user/list/2/name")).map(_.value shouldBe response1)
      _ <- repository.get(Request(Method.GET, "/user/list/1/name")).map(_.value shouldBe response1)
      _ <- repository.get(Request(Method.GET, "/user/list/1/status")).map(_.value shouldBe response2)
    } yield ()
  }

  it should "wildcard should take precedence over multi wildcard" in {
    val response1 = Response.withJsonBody(Json.fromString("OK - 1"), StatusCode.Ok)
    val response2 = Response.withJsonBody(Json.fromString("Ok - 2"), StatusCode.Ok)
    val response3 = Response.withJsonBody(Json.fromString("Ok - 3"), StatusCode.Ok)
    for {
      repository <- StubRepository()
      _ <- repository
        .put(RequestStub(MethodStub.FixedMethod(Method.GET), "/user/**"), NonEmptyList.one(response1))
      _ <- repository
        .put(RequestStub(MethodStub.FixedMethod(Method.GET), "/user/*/status"), NonEmptyList.one(response2))
      _ <- repository
        .put(RequestStub(MethodStub.FixedMethod(Method.GET), "/user/4/status"), NonEmptyList.one(response3))
      _ <- repository.get(Request(Method.GET, "/user/1/name")).map(_.value shouldBe response1)
      _ <- repository.get(Request(Method.GET, "/user/accept")).map(_.value shouldBe response1)
      _ <- repository.get(Request(Method.GET, "/user/1/status/param1")).map(_.value shouldBe response1)
      _ <- repository.get(Request(Method.GET, "/user/1/status")).map(_.value shouldBe response2)
      _ <- repository.get(Request(Method.GET, "/user/4/status")).map(_.value shouldBe response3)
    } yield ()
  }

  it should "support query params" in {
    val response = Response.withJsonBody(Json.fromString("OK"), StatusCode.Ok)
    for {
      repository <- StubRepository()
      _ <- repository
        .put(RequestStub(MethodStub.FixedMethod(Method.GET), "/admin?filter=true"), NonEmptyList.one(response))
      _ <- repository.get(Request(Method.GET, "/admin?filter=true")).map(_.value shouldBe response)
      _ <- repository.get(Request(Method.GET, "/admin?filter=false")).map(_ shouldBe empty)
    } yield ()
  }

  it should "support wildcard for query params" in {
    val response = Response.withJsonBody(Json.fromString("OK"), StatusCode.Ok)
    for {
      repository <- StubRepository()
      _ <- repository
        .put(RequestStub(MethodStub.FixedMethod(Method.GET), "/admin?filter=*"), NonEmptyList.one(response))
      _ <- repository.get(Request(Method.GET, "/admin?filter=true")).map(_.value shouldBe response)
      _ <- repository.get(Request(Method.GET, "/admin?filter=false")).map(_.value shouldBe response)
    } yield ()
  }

  it should "support multiple queries" in {
    val response = Response.withJsonBody(Json.fromString("OK"), StatusCode.Ok)
    for {
      repository <- StubRepository()
      _ <- repository
        .put(
          RequestStub(MethodStub.FixedMethod(Method.GET), "/admin?filter=true&multi=false"),
          NonEmptyList.one(response)
        )
      _ <- repository.get(Request(Method.GET, "/admin?filter=true&multi=false")).map(_.value shouldBe response)
      _ <- repository.get(Request(Method.GET, "/admin?filter=false&multi=false")).map(_ shouldBe empty)
    } yield ()
  }

  it should "not take query order into account" in {
    val response = Response.withJsonBody(Json.fromString("OK"), StatusCode.Ok)
    for {
      repository <- StubRepository()
      _ <- repository
        .put(
          RequestStub(MethodStub.FixedMethod(Method.GET), "/admin?filter=true&multi=false"),
          NonEmptyList.one(response)
        )
      _ <- repository.get(Request(Method.GET, "/admin?multi=false&filter=true")).map(_.value shouldBe response)
      _ <- repository.get(Request(Method.GET, "/admin?multi=false&filter=false")).map(_ shouldBe empty)
    } yield ()
  }

  it should "support query with multiple values" in {
    val response = Response.withJsonBody(Json.fromString("OK"), StatusCode.Ok)
    for {
      repository <- StubRepository()
      _ <- repository
        .put(RequestStub(MethodStub.FixedMethod(Method.GET), "/admin?p=1&p=2"), NonEmptyList.one(response))
      _ <- repository.get(Request(Method.GET, "/admin?p=1&p=2")).map(_.value shouldBe response)
      _ <- repository.get(Request(Method.GET, "/admin?p=1&p=3")).map(_ shouldBe empty)
    } yield ()
  }

  it should "support value wildcard with multi value query" in {
    val response = Response.withJsonBody(Json.fromString("OK"), StatusCode.Ok)
    for {
      repository <- StubRepository()
      _ <- repository
        .put(RequestStub(MethodStub.FixedMethod(Method.GET), "/admin?p=*"), NonEmptyList.one(response))
      _ <- repository.get(Request(Method.GET, "/admin?p=1&p=2")).map(_.value shouldBe response)
    } yield ()
  }

  it should "support wildcard query" in {
    val response = Response.withJsonBody(Json.fromString("OK"), StatusCode.Ok)
    for {
      repository <- StubRepository()
      _ <- repository
        .put(RequestStub(MethodStub.FixedMethod(Method.GET), "/admin?*"), NonEmptyList.one(response))
      _ <- repository.get(Request(Method.GET, "/admin?p=1&p=2&q=3")).map(_.value shouldBe response)
    } yield ()
  }

  it should "support optional wildcard query" in {
    val response = Response.withJsonBody(Json.fromString("OK"), StatusCode.Ok)
    for {
      repository <- StubRepository()
      _ <- repository
        .put(
          RequestStub(
            MethodStub.FixedMethod(Method.GET),
            RequestPathAndQuery(
              PathStub(List(PathElement.Fixed("admin"))),
              QueryStub(ListSet(QueryElement.WildcardValueQuery("p", isRequired = false)))
            )
          ),
          NonEmptyList.one(response)
        )
      _ <- repository.get(Request(Method.GET, "/admin?p=1")).map(_.value shouldBe response)
      _ <- repository.get(Request(Method.GET, "/admin")).map(_.value shouldBe response)
    } yield ()
  }

  it should "both method, path and query should make routes distinctive" in {
    val response = Response.withJsonBody(Json.fromString("OK - 1"), StatusCode.Ok)
    for {
      repository <- StubRepository()
      _ <- repository
        .put(
          RequestStub(
            MethodStub.FixedMethod(Method.GET),
            RequestPathAndQuery(PathStub(List(PathElement.Fixed("admin"))), QueryStub(ListSet()))
          ),
          NonEmptyList.one(response)
        )
      _ <- repository
        .put(
          RequestStub(
            MethodStub.FixedMethod(Method.POST),
            RequestPathAndQuery(
              PathStub(List(PathElement.Fixed("admin"))),
              QueryStub(ListSet(QueryElement.WildcardValueQuery("p", isRequired = false)))
            )
          ),
          NonEmptyList.one(response)
        )
      _ <- repository.get(Request(Method.POST, "/admin?p=1")).map(_.value shouldBe response)
      _ <- repository.get(Request(Method.GET, "/admin")).map(_.value shouldBe response)
    } yield ()
  }

  it should "cycle through responses" in {
    val response1 = Response.withJsonBody(Json.fromString("1"), StatusCode.Ok)
    val response2 = Response.withJsonBody(Json.fromString("2"), StatusCode.Ok)
    for {
      repository <- StubRepository()
      _ <- repository
        .put(RequestStub(MethodStub.FixedMethod(Method.GET), "/admin"), NonEmptyList.of(response1, response2))
      _ <- (1 to 5).toList.traverse { _ =>
        repository.get(Request(Method.GET, "/admin")).map(_.value shouldBe response1) >>
          repository.get(Request(Method.GET, "/admin")).map(_.value shouldBe response2)
      }
    } yield ()
  }
}
