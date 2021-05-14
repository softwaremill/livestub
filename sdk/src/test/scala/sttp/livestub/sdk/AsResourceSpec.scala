package sttp.livestub.sdk

import cats.effect.testing.scalatest.{AsyncIOSpec, CatsResource, CatsResourceIO}
import io.circe.Json
import org.scalatest.freespec.FixtureAsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import sttp.client3._
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.livestub.api.{MethodStub, RequestStubIn, Response}
import sttp.model.{Header, StatusCode}
import sttp.tapir.Tapir
import cats.effect._
import cats.implicits.catsSyntaxApplicativeId
import org.scalatest.Assertion
import sttp.livestub.app.LiveStubServer
import sttp.livestub.app.LiveStubServer.Config
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.ExecutionContext
import cats.syntax.all._

class AsResourceSpec
    extends FixtureAsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with Tapir
    with CatsResourceIO[SttpBackend[IO, Any]] {
  private implicit val cs = IO.contextShift(ExecutionContext.global)

  override def resource: Resource[IO, SttpBackend[IO, Any]] =
    LiveStubServer.resource(Config(port = 7070)) >> AsyncHttpClientCatsBackend.resource[IO]()

  val request = basicRequest
    .body(Map("name" -> "John", "surname" -> "doe"))
    .post(uri"http://localhost:7070/post?signup=123")

  "e2e test case" in { implicit backend: SttpBackend[IO, Any] =>
    val sdk = new LiveStubSdk[IO](uri"http://localhost:7070")

    sdk.when(request).thenRespondR(Response(None, StatusCode.Ok)).use { _ =>
      request.send(backend).map(response => response.code shouldBe StatusCode.Ok)
    }

  }
}
