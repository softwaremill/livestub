package sttp.livestub.api
import io.circe.{Decoder, Encoder}
import io.circe.generic.extras.{AutoDerivation, Configuration}
import sttp.model.{Method, StatusCode}

trait JsonSupport extends AutoDerivation {
  implicit val config: Configuration = Configuration.default.withDefaults
}
