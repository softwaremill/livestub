package sttp.livestub.api

import io.circe.Encoder
import sttp.model._

object LiveStubApi extends LiveStubTapirSupport with JsonSupport {
  val a = implicitly[Encoder[Header]]
}
