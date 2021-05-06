package sttp.livestub.openapi

import io.circe.yaml.parser
import sttp.livestub.openapi.OpenapiModels.OpenapiDocument
import cats.implicits._
import io.circe._

object YamlParser {

  def parseFile(yamlString: String): Either[Error, OpenapiDocument] = {
    parser
      .parse(yamlString)
      .leftMap(err => err: Error)
      .flatMap(_.as[OpenapiDocument])
  }
}
