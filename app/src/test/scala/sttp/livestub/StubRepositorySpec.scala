package sttp.livestub

import io.circe.Json
import org.scalatest.OptionValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.livestub.api._
import sttp.model.{Method, StatusCode}

class StubRepositorySpec extends AnyFlatSpec with Matchers with OptionValues {

  "StubRepositorySpec" should "return response for single fragment path" in {
    val repository = StubsRepositoryImpl()
    val response = Response(Json.fromString("OK"), StatusCode.Ok)
    repository.put(RequestStub(MethodValue.FixedMethod(Method.GET), "/admin"), response).unsafeRunSync()

    repository.get(Request(Method.GET, "/admin")).unsafeRunSync().value shouldBe response
  }

  it should "return none for not matching path" in {
    val repository = StubsRepositoryImpl()

    repository.get(Request(Method.GET, "/admin")).unsafeRunSync() shouldBe empty
  }

  it should "return response for multi fragment path" in {
    val repository = StubsRepositoryImpl()
    val response = Response(Json.fromString("OK"), StatusCode.Ok)
    repository
      .put(RequestStub(MethodValue.FixedMethod(Method.GET), "/admin/docs"), response)
      .unsafeRunSync()

    repository.get(Request(Method.GET, "/admin/docs")).unsafeRunSync().value shouldBe response
  }

  it should "distinguish when path contains another subpath" in {
    val repository = StubsRepositoryImpl()
    val response1 = Response(Json.fromString("OK"), StatusCode.Ok)
    val response2 = Response(Json.fromString("Bad"), StatusCode.BadRequest)
    repository
      .put(RequestStub(MethodValue.FixedMethod(Method.GET), "/admin/docs"), response1)
      .unsafeRunSync()
    repository.put(RequestStub(MethodValue.FixedMethod(Method.GET), "/admin"), response2).unsafeRunSync()

    repository.get(Request(Method.GET, "/admin/docs")).unsafeRunSync().value shouldBe response1
    repository.get(Request(Method.GET, "/admin")).unsafeRunSync().value shouldBe response2
  }

  it should "support wildcard paths" in {
    val repository = StubsRepositoryImpl()
    val response = Response(Json.fromString("OK"), StatusCode.Ok)
    repository.put(RequestStub(MethodValue.FixedMethod(Method.GET), "/*"), response).unsafeRunSync()

    repository.get(Request(Method.GET, "/docs")).unsafeRunSync().value shouldBe response
    repository.get(Request(Method.GET, "/admin")).unsafeRunSync().value shouldBe response
  }

  it should "distinguish between wildcard path and direct path" in {
    val repository = StubsRepositoryImpl()
    val response1 = Response(Json.fromString("OK"), StatusCode.Ok)
    val response2 = Response(Json.fromString("Bad"), StatusCode.BadRequest)
    repository
      .put(RequestStub(MethodValue.FixedMethod(Method.GET), "/admin/*"), response1)
      .unsafeRunSync()
    repository.put(RequestStub(MethodValue.FixedMethod(Method.GET), "/admin"), response2).unsafeRunSync()

    repository.get(Request(Method.GET, "/admin/docs")).unsafeRunSync().value shouldBe response1
    repository.get(Request(Method.GET, "/admin")).unsafeRunSync().value shouldBe response2
  }

  it should "support wildcard methods" in {
    val repository = StubsRepositoryImpl()
    val response = Response(Json.fromString("OK"), StatusCode.Ok)
    repository.put(RequestStub(MethodValue.Wildcard, "/admin"), response).unsafeRunSync()

    repository.get(Request(Method.POST, "/admin")).unsafeRunSync().value shouldBe response
    repository.get(Request(Method.GET, "/admin")).unsafeRunSync().value shouldBe response
  }

  it should "only resolve wildcard path elements on a single level" in {
    val repository = StubsRepositoryImpl()
    val response = Response(Json.fromString("OK"), StatusCode.Ok)
    repository
      .put(RequestStub(MethodValue.FixedMethod(Method.GET), "/admin/*"), response)
      .unsafeRunSync()

    repository.get(Request(Method.GET, "/admin/docs")).unsafeRunSync().value shouldBe response
    repository.get(Request(Method.GET, "/admin/docs/status")).unsafeRunSync() shouldBe empty
  }

  it should "resolve multi wildcards globally" in {
    val repository = StubsRepositoryImpl()
    val response = Response(Json.fromString("OK"), StatusCode.Ok)
    repository
      .put(RequestStub(MethodValue.FixedMethod(Method.GET), "/admin/**"), response)
      .unsafeRunSync()

    repository.get(Request(Method.GET, "/admin/docs")).unsafeRunSync().value shouldBe response
    repository.get(Request(Method.GET, "/admin/docs/status")).unsafeRunSync().value shouldBe response
  }

  it should "recursively search for multi wildcard in upstream routes" in {
    val repository = StubsRepositoryImpl()
    val response1 = Response(Json.fromString("OK"), StatusCode.Ok)
    val response2 = Response(Json.fromString("..."), StatusCode.BadGateway)
    repository
      .put(RequestStub(MethodValue.FixedMethod(Method.GET), "/user/**"), response1)
      .unsafeRunSync()

    repository
      .put(RequestStub(MethodValue.FixedMethod(Method.GET), "/user/list/1/status"), response2)
      .unsafeRunSync()

    repository.get(Request(Method.GET, "/user/list/2/name")).unsafeRunSync().value shouldBe response1
    repository.get(Request(Method.GET, "/user/list/1/name")).unsafeRunSync().value shouldBe response1
    repository.get(Request(Method.GET, "/user/list/1/status")).unsafeRunSync().value shouldBe response2
  }

  it should "support combining wildcard with multi wildcard" in {
    val repository = StubsRepositoryImpl()
    val response1 = Response(Json.fromString("OK - 1"), StatusCode.Ok)
    val response2 = Response(Json.fromString("OK - 2"), StatusCode.Ok)
    val response3 = Response(Json.fromString("OK - 3"), StatusCode.Ok)
    repository
      .put(RequestStub(MethodValue.FixedMethod(Method.GET), "/user/**"), response1)
      .unsafeRunSync()

    repository
      .put(RequestStub(MethodValue.FixedMethod(Method.GET), "/user/*/status"), response2)
      .unsafeRunSync()

    repository
      .put(RequestStub(MethodValue.FixedMethod(Method.GET), "/user/4/status"), response3)
      .unsafeRunSync()

    repository.get(Request(Method.GET, "/user/1/name")).unsafeRunSync().value shouldBe response1
    repository.get(Request(Method.GET, "/user/accept")).unsafeRunSync().value shouldBe response1
    repository.get(Request(Method.GET, "/user/1/status/param1")).unsafeRunSync().value shouldBe response1
    repository.get(Request(Method.GET, "/user/1/status")).unsafeRunSync().value shouldBe response2
    repository.get(Request(Method.GET, "/user/4/status")).unsafeRunSync().value shouldBe response3

  }

  it should "support query params" in {
    val repository = StubsRepositoryImpl()
    val response = Response(Json.fromString("OK"), StatusCode.Ok)
    repository
      .put(RequestStub(MethodValue.FixedMethod(Method.GET), "/admin?filter=true"), response)
      .unsafeRunSync()

    repository.get(Request(Method.GET, "/admin?filter=true")).unsafeRunSync().value shouldBe response
    repository.get(Request(Method.GET, "/admin?filter=false")).unsafeRunSync() shouldBe empty
  }

  it should "support wildcard for query params" in {
    val repository = StubsRepositoryImpl()
    val response = Response(Json.fromString("OK"), StatusCode.Ok)
    repository
      .put(RequestStub(MethodValue.FixedMethod(Method.GET), "/admin?filter=*"), response)
      .unsafeRunSync()

    repository.get(Request(Method.GET, "/admin?filter=true")).unsafeRunSync().value shouldBe response
    repository.get(Request(Method.GET, "/admin?filter=false")).unsafeRunSync().value shouldBe response
  }

  it should "support multiple queries" in {
    val repository = StubsRepositoryImpl()
    val response = Response(Json.fromString("OK"), StatusCode.Ok)
    repository
      .put(RequestStub(MethodValue.FixedMethod(Method.GET), "/admin?filter=true&multi=false"), response)
      .unsafeRunSync()

    repository.get(Request(Method.GET, "/admin?filter=true&multi=false")).unsafeRunSync().value shouldBe response
    repository.get(Request(Method.GET, "/admin?filter=false&multi=false")).unsafeRunSync() shouldBe empty
  }

  it should "not take query order into account" in {
    val repository = StubsRepositoryImpl()
    val response = Response(Json.fromString("OK"), StatusCode.Ok)
    repository
      .put(RequestStub(MethodValue.FixedMethod(Method.GET), "/admin?filter=true&multi=false"), response)
      .unsafeRunSync()

    repository.get(Request(Method.GET, "/admin?multi=false&filter=true")).unsafeRunSync().value shouldBe response
    repository.get(Request(Method.GET, "/admin?multi=false&filter=false")).unsafeRunSync() shouldBe empty
  }
}
