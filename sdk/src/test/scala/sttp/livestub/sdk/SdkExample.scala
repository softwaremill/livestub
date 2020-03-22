package sttp.livestub.sdk

import cats.effect.IO
import io.circe.Json
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import sttp.client.{SttpBackend, _}
import sttp.client.asynchttpclient.WebSocketHandler
import sttp.client.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.model.StatusCode
import sttp.tapir.Tapir

import scala.concurrent.ExecutionContext

class SdkExample extends AnyFreeSpec with Matchers with Tapir {

  private implicit val cs = IO.contextShift(ExecutionContext.global)

  "sttp example" in {
    AsyncHttpClientCatsBackend[IO]().flatMap { implicit backend: SttpBackend[IO, Nothing, WebSocketHandler] =>
      val request = basicRequest
        .body(Map("name" -> "John", "surname" -> "doe"))
        .post(uri"https://httpbin.org/post?signup=123")

      val livestub = new LiveStubSdk(uri"http://mock:7070")
      livestub.when(request).`then`(StatusCode.Ok, Json.fromString("OK"))
    }
  }

  "tapir example" in {
    AsyncHttpClientCatsBackend[IO]().flatMap { implicit backend: SttpBackend[IO, Nothing, WebSocketHandler] =>
      val myEndpoint = endpoint.get.in("/status").out(stringBody)

      val livestub = new LiveStubSdk(uri"http://mock:7070")
      livestub.when(myEndpoint).`then`(StatusCode.Ok, Json.fromString("OK"))
    }
  }
}
