package sttp.livestub

import cats.syntax.all._
import com.softwaremill.tagging.@@
import com.typesafe.scalalogging.StrictLogging
import io.circe.Json
import sttp.livestub.RandomValueGenerator.Seed
import sttp.livestub.openapi.OpenapiModels.OpenapiDocument
import sttp.livestub.openapi.OpenapiSchemaType

import java.time.{LocalDate, LocalDateTime}
import java.util.UUID
import scala.util.Random

class RandomValueGenerator(spec: OpenapiDocument, seed: Option[Seed]) extends StrictLogging {
  private lazy val random = seed match {
    case Some(value) => new Random(value)
    case None =>
      val newSeed = new Random().nextLong()
      logger.info(s"Initializing random data generator with following seed: $newSeed")
      new Random(newSeed)
  }

  def nextRandom(schema: OpenapiSchemaType): Either[String, Json] = {
    schema match {
      case OpenapiSchemaType.OpenapiSchemaBoolean(_, example) =>
        Right(Json.fromBoolean(example.getOrElse(random.nextBoolean())))
      case OpenapiSchemaType.OpenapiSchemaString(_, example) =>
        Right(Json.fromString(example.getOrElse(UUID.randomUUID().toString)))
      case OpenapiSchemaType.OpenapiSchemaDate(_, example) =>
        Right(Json.fromString(example.getOrElse(LocalDate.now().toString)))
      case OpenapiSchemaType.OpenapiSchemaDateTime(_, example) =>
        Right(Json.fromString(example.getOrElse(LocalDateTime.now().toString)))
      case OpenapiSchemaType.OpenapiSchemaInt(_, example)  => Right(Json.fromInt(example.getOrElse(random.nextInt())))
      case OpenapiSchemaType.OpenapiSchemaLong(_, example) => Right(Json.fromLong(example.getOrElse(random.nextLong())))
      case OpenapiSchemaType.OpenapiSchemaDouble(_, example) =>
        Json.fromDouble(example.getOrElse(random.nextDouble())).fold("wrong double".asLeft[Json])(Right(_))
      case OpenapiSchemaType.OpenapiSchemaFloat(_, example) =>
        Json.fromFloat(example.getOrElse(random.nextFloat())).fold("wrong float".asLeft[Json])(Right(_))
      case OpenapiSchemaType.OpenapiSchemaArray(items, _) =>
        nextRandom(items).map(item => Json.arr(item, item, item))
      case OpenapiSchemaType.OpenapiSchemaObject(properties, _, _, example, _) =>
        example.map(Right(_)).getOrElse(createObject(properties))
      case OpenapiSchemaType.OpenapiSchemaRef(ref) =>
        nextRandom(spec.components.schemas(ref.split("/").last))
      case other => Left(s"Couldn't provide example value for $other")
    }
  }

  def createObject(properties: Map[String, OpenapiSchemaType]): Either[String, Json] = {
    properties.toList
      .traverse { case (k, v) =>
        nextRandom(v).map(j => k -> j)
      }
      .map(jsonProps => Json.obj(jsonProps: _*))
  }
}
object RandomValueGenerator {
  type SeedTag
  type Seed = Long @@ SeedTag
}
