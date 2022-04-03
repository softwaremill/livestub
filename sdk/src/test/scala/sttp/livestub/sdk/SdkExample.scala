package sttp.livestub.sdk

import cats.effect.IO
import io.circe.Json
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.client3._
import sttp.livestub.api.{MethodStub, RequestStubIn, Response}
import sttp.model.StatusCode
import sttp.model.Header
import sttp.tapir.Tapir

class SdkExample extends AnyFreeSpec with Matchers with Tapir {

  /** These tests are not executing intentionally, as they serve only as examples. They are here to verify compilation
    */

  "sttp example" in {
    AsyncHttpClientCatsBackend[IO]().flatMap { backend: SttpBackend[IO, Any] =>
      val request = basicRequest
        .body(Map("name" -> "John", "surname" -> "doe"))
        .post(uri"https://httpbin.org/post?signup=123")

      val livestub = new LiveStubSdk[IO](uri"http://mock:7070", backend)
      livestub.when(request).thenRespond(Response(Some(Json.fromString("OK")), StatusCode.Ok))
    }
  }

  "tapir example" in {
    AsyncHttpClientCatsBackend[IO]().flatMap { backend: SttpBackend[IO, Any] =>
      val myEndpoint = endpoint.get.in("/status").out(stringBody)

      val livestub = new LiveStubSdk[IO](uri"http://mock:7070", backend)
      livestub
        .when(myEndpoint)
        .thenRespond(Response.emptyBody(StatusCode.Ok, List(Header("X-App", "123"))))
    }
  }

  "example" in {
    AsyncHttpClientCatsBackend[IO]().flatMap { backend: SttpBackend[IO, Any] =>
      val livestub = new LiveStubSdk[IO](uri"http://mock:7070", backend)
      livestub
        .when(RequestStubIn(MethodStub.Wildcard, "/user/*/status"))
        .thenRespond(Response.emptyBody(StatusCode.Ok, List(Header("X-App", "123"))))
    }
  }
}
