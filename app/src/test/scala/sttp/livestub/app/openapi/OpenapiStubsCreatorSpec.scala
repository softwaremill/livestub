package sttp.livestub.app.openapi

import com.softwaremill.tagging.Tagger
import io.circe._
import io.circe.parser._
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.livestub.api._
import sttp.livestub.app.openapi.OpenapiStubsCreatorSpec.{CategoryComponent, PetComponent, TagComponent}
import sttp.livestub.openapi.OpenapiModels.ResponseStatusCode.Fixed
import sttp.livestub.openapi.OpenapiModels.{
  OpenapiParameter,
  OpenapiPath,
  OpenapiPathMethod,
  OpenapiResponse,
  OpenapiResponseContent
}
import sttp.livestub.openapi.OpenapiParamType
import sttp.livestub.openapi.OpenapiSchemaType.{
  OpenapiSchemaArray,
  OpenapiSchemaLong,
  OpenapiSchemaObject,
  OpenapiSchemaRef,
  OpenapiSchemaString
}
import sttp.model.{MediaType, Method, StatusCode}

import scala.collection.immutable.ListSet

class OpenapiStubsCreatorSpec extends AnyFlatSpec with Matchers with EitherValues {
  val creator = new OpenapiStubsCreator(
    new RandomValueGenerator(
      Map(PetComponent, CategoryComponent, TagComponent),
      Some(123L.taggedWith[RandomValueGenerator.SeedTag])
    )
  )

  it should "convert /pet/findByStatus to request stub with body" in {
    val paths = List(
      OpenapiPath(
        "/pet/findByStatus",
        List(
          OpenapiPathMethod(
            Method.GET,
            List(
              OpenapiParameter(
                "status",
                OpenapiParamType.Query,
                Some(true),
                Some("Status values that need to be considered for filter"),
                OpenapiSchemaString(nullable = false, None)
              )
            ),
            List(
              OpenapiResponse(
                Fixed(StatusCode.Ok),
                "successful operation",
                List(
                  OpenapiResponseContent(
                    MediaType.ApplicationJson,
                    OpenapiSchemaArray(OpenapiSchemaRef("#/components/schemas/Pet"), nullable = false)
                  )
                )
              ),
              OpenapiResponse(Fixed(StatusCode.BadRequest), "Invalid status value", List())
            ),
            None,
            Some("Finds Pets by status")
          )
        )
      )
    )
    val generatedBody: Json = parse("""
    [{
    "name" : "doggie",
    "tags" : [
      {
        "id" : -5106534569952410475,
        "name" : "tag1"
      }
    ],
    "photoUrls" : [
      "http://random-photo-url.com"
    ],
    "id" : -167885730524958550,
    "status" : "available",
    "category" : {
      "id" : 4672433029010564658,
      "name" : "Dogs"
    }
  }]""").value
    creator(paths) shouldBe List(
      RequestStubIn(MethodStub.FixedMethod(Method.GET), "/pet/findByStatus?status=*") -> Response(
        Some(generatedBody),
        StatusCode.Ok,
        List.empty
      )
    )
  }

  it should "convert /pet/findByTags to request stub with body" in {
    val paths = List(
      OpenapiPath(
        "/pet/findByTags",
        List(
          OpenapiPathMethod(
            Method.GET,
            List(
              OpenapiParameter(
                "tags",
                OpenapiParamType.Query,
                Some(false),
                None,
                OpenapiSchemaString(nullable = false, None)
              )
            ),
            List(
              OpenapiResponse(
                Fixed(StatusCode.Ok),
                "successful operation",
                List(
                  OpenapiResponseContent(
                    MediaType.ApplicationJson,
                    OpenapiSchemaArray(OpenapiSchemaRef("#/components/schemas/Pet"), nullable = false)
                  )
                )
              ),
              OpenapiResponse(Fixed(StatusCode.BadRequest), "Invalid tags value", List())
            ),
            None,
            Some("Finds Pets by tags")
          )
        )
      )
    )
    val generatedBody: Json = parse("""
    [{
    "name" : "doggie",
    "tags" : [
      {
        "id" : -7216359497931550918,
        "name" : "tag1"
      }
    ],
    "photoUrls" : [
      "http://random-photo-url.com"
    ],
    "id" : -3581075550420886390,
    "status" : "available",
    "category" : {
      "id" : -2298228485105199876,
      "name" : "Dogs"
    }
  }]""").value
    creator(paths) shouldBe List(
      RequestStubIn(
        MethodStub.FixedMethod(Method.GET),
        RequestPathAndQuery(
          List(PathElement.Fixed("pet"), PathElement.Fixed("findByTags")),
          ListSet(QueryElement.WildcardValueQuery("tags", isRequired = false))
        )
      ) -> Response(
        Some(generatedBody),
        StatusCode.Ok,
        List.empty
      )
    )
  }

  it should "convert updatePetWithForm to stub" in {
    val path = OpenapiPath(
      "/pet/{id}",
      List(
        OpenapiPathMethod(
          Method.POST,
          List(
            OpenapiParameter(
              "petId",
              OpenapiParamType.Path,
              Some(true),
              Some("ID of pet that needs to be updated"),
              OpenapiSchemaLong(nullable = false, None)
            ),
            OpenapiParameter(
              "name",
              OpenapiParamType.Query,
              None,
              Some("Name of pet that needs to be updated"),
              OpenapiSchemaString(nullable = false, None)
            ),
            OpenapiParameter(
              "status",
              OpenapiParamType.Query,
              None,
              Some("Status of pet that needs to be updated"),
              OpenapiSchemaString(nullable = false, None)
            )
          ),
          List(OpenapiResponse(Fixed(StatusCode.MethodNotAllowed), "Invalid input", List())),
          None,
          Some("Updates a pet in the store with form data")
        )
      )
    )
    creator(List(path)) shouldBe List(
      RequestStubIn(
        MethodStub.FixedMethod(Method.POST),
        RequestPathAndQuery(
          List(PathElement.Fixed("pet"), PathElement.Wildcard),
          ListSet(
            QueryElement.WildcardValueQuery("name", isRequired = false),
            QueryElement.WildcardValueQuery("status", isRequired = false)
          )
        )
      ) -> Response(
        None,
        StatusCode.MethodNotAllowed,
        List.empty
      )
    )
  }

}
object OpenapiStubsCreatorSpec {
  val PetComponent: (String, OpenapiSchemaObject) = "Pet" -> OpenapiSchemaObject(
    properties = Map(
      "name" -> OpenapiSchemaString(nullable = false, Some("doggie")),
      "tags" -> OpenapiSchemaArray(OpenapiSchemaRef("#/components/schemas/Tag"), nullable = false),
      "photoUrls" -> OpenapiSchemaArray(
        OpenapiSchemaString(nullable = false, Some("http://random-photo-url.com")),
        nullable = false
      ),
      "id" -> OpenapiSchemaLong(nullable = false, None),
      "status" -> OpenapiSchemaString(nullable = false, Some("available")),
      "category" -> OpenapiSchemaRef("#/components/schemas/Category")
    ),
    required = List("name", "photoUrls"),
    nullable = false,
    example = None,
    additionalProperties = Left(false)
  )
  val CategoryComponent: (String, OpenapiSchemaObject) = "Category" -> OpenapiSchemaObject(
    Map(
      "id" -> OpenapiSchemaLong(nullable = false, None),
      "name" -> OpenapiSchemaString(nullable = false, Some("Dogs"))
    ),
    List.empty,
    nullable = false,
    example = None,
    additionalProperties = Left(false)
  )
  val TagComponent: (String, OpenapiSchemaObject) = "Tag" -> OpenapiSchemaObject(
    properties = Map(
      "id" -> OpenapiSchemaLong(nullable = false, None),
      "name" -> OpenapiSchemaString(nullable = false, Some("tag1"))
    ),
    required = Seq.empty,
    nullable = false,
    example = None,
    additionalProperties = Left(false)
  )
}
