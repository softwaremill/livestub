package sttp.livestub.sdk

import cats.effect.IO
import io.circe.Json
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.client3._
import sttp.livestub.api.{MethodStub, RequestStub, Response}
import sttp.model.StatusCode
import sttp.model.Header
import sttp.tapir.Tapir

import scala.concurrent.ExecutionContext

class SdkExample extends AnyFreeSpec with Matchers with Tapir {

  private implicit val cs = IO.contextShift(ExecutionContext.global)

  "sttp example" in {
    AsyncHttpClientCatsBackend[IO]().flatMap { implicit backend: SttpBackend[IO, Any] =>
      val request = basicRequest
        .body(Map("name" -> "John", "surname" -> "doe"))
        .post(uri"https://httpbin.org/post?signup=123")

      val livestub = new LiveStubSdk[IO](uri"http://mock:7070")
      livestub.when(request).thenRespond(Response(Some(Json.fromString("OK")), StatusCode.Ok))
    }
  }

  "tapir example" in {
    AsyncHttpClientCatsBackend[IO]().flatMap { implicit backend: SttpBackend[IO, Any] =>
      val myEndpoint = endpoint.get.in("/status").out(stringBody)

      val livestub = new LiveStubSdk[IO](uri"http://mock:7070")
      livestub.when(myEndpoint).thenRespond(Response.emptyBody(StatusCode.Ok, List(Header("X-App", "123"))))
    }
  }

  "example" in {
    AsyncHttpClientCatsBackend[IO]().flatMap { implicit backend: SttpBackend[IO, Any] =>
      val livestub = new LiveStubSdk[IO](uri"http://mock:7070")
      livestub
        .when(RequestStub(MethodStub.Wildcard, "/user/*/status"))
        .thenRespond(Response.emptyBody(StatusCode.Ok, List(Header("X-App", "123"))))
    }
  }
}
