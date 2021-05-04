package sttp.livestub.openapi

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class OpenapiParserSpec extends AnyFlatSpec with Matchers {
  it should "parse correctly petstore specification" in {
    val str = FileLoader.loadFile("petstore.yaml")
    val openapi = YamlParser.parseFile(str)
    openapi match {
      case Left(value)  => fail(value)
      case Right(value) => println(value)
    }
  }
}
