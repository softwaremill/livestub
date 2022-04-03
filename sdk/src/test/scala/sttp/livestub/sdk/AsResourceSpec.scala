package sttp.livestub.sdk

import cats.effect._
import cats.effect.testing.scalatest.AsyncIOSpec
import org.http4s.Uri
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import sttp.client3._
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.livestub.api.Response
import sttp.livestub.app.LiveStubServer
import sttp.livestub.app.LiveStubServer.Config
import sttp.model.StatusCode
import sttp.tapir.Tapir

class AsResourceSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with Tapir {

  val resource: Resource[IO, (Uri, SttpBackend[IO, Any])] =
    for {
      baseUri <- LiveStubServer.resource(Config(port = 0)).map(_.baseUri)
      backend <- AsyncHttpClientCatsBackend.resource[IO]()
    } yield (baseUri, backend)

  private def request(baseUri: sttp.model.Uri) = basicRequest
    .body(Map("name" -> "John", "surname" -> "doe"))
    .post(uri"$baseUri/post?signup=123")

  "e2e test case" in {
    resource.use { case (baseUri, backend) =>
      val sttpUri = sttp.model.Uri.parse(baseUri.renderString).getOrElse(???)
      val sdk = new LiveStubSdk[IO](sttpUri, backend)

      sdk
        .when(request(sttpUri))
        .thenRespondR(Response(None, StatusCode.Ok))
        .use { _ =>
          request(sttpUri).send(backend).map(response => response.code shouldBe StatusCode.Ok)
        }
    }
  }
}
