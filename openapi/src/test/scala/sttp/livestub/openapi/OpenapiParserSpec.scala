package sttp.livestub.openapi

import com.softwaremill.diffx.Diff
import com.softwaremill.diffx.scalatest.DiffMatcher
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.livestub.openapi.OpenapiModels.ResponseStatusCode.Fixed
import sttp.livestub.openapi.OpenapiModels.{
  OpenapiDocument,
  OpenapiInfo,
  OpenapiParameter,
  OpenapiPath,
  OpenapiPathMethod,
  OpenapiRequestBody,
  OpenapiRequestBodyContent,
  OpenapiResponse,
  OpenapiResponseContent
}
import sttp.livestub.openapi.OpenapiSchemaType.{
  OpenapiSchemaArray,
  OpenapiSchemaBinary,
  OpenapiSchemaBoolean,
  OpenapiSchemaDateTime,
  OpenapiSchemaInt,
  OpenapiSchemaLong,
  OpenapiSchemaObject,
  OpenapiSchemaRef,
  OpenapiSchemaString
}
import sttp.model.{MediaType, Method, StatusCode}

class OpenapiParserSpec extends AnyFlatSpec with Matchers with EitherValues with DiffMatcher with DiffInstances {
  it should "parse correctly petstore specification" in {
    val str = FileLoader.loadFile("petstore.yaml")
    val openapi = YamlParser.parseFile(str).value
    Diff[OpenapiInfo]
    openapi should matchTo(
      OpenapiDocument(
        "3.0.2",
        OpenapiInfo("Swagger Petstore - OpenAPI 3.0", "1.0.5"),
        List(
          OpenapiPath(
            "/store/order",
            List(
              OpenapiPathMethod(
                Method.POST,
                List.empty,
                List(
                  OpenapiResponse(
                    Fixed(StatusCode.Ok),
                    "successful operation",
                    List(
                      OpenapiResponseContent(MediaType.ApplicationJson, OpenapiSchemaRef("#/components/schemas/Order"))
                    )
                  ),
                  OpenapiResponse(Fixed(StatusCode.MethodNotAllowed), "Invalid input", List())
                ),
                Some(
                  OpenapiRequestBody(
                    None,
                    None,
                    List(
                      OpenapiRequestBodyContent(
                        MediaType.ApplicationJson,
                        OpenapiSchemaRef("#/components/schemas/Order")
                      )
                    )
                  )
                ),
                Some("Place an order for a pet")
              )
            )
          ),
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
                    Some("Tags to filter by"),
                    OpenapiSchemaArray(OpenapiSchemaString(nullable = false, None), nullable = false)
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
                  OpenapiResponse(Fixed(StatusCode.BadRequest), "Invalid tag value", List())
                ),
                None,
                Some("Finds Pets by tags")
              )
            )
          ),
          OpenapiPath(
            "/pet",
            List(
              OpenapiPathMethod(
                Method.PUT,
                List(),
                List(
                  OpenapiResponse(
                    Fixed(StatusCode.Ok),
                    "Successful operation",
                    List(
                      OpenapiResponseContent(MediaType.ApplicationJson, OpenapiSchemaRef("#/components/schemas/Pet"))
                    )
                  ),
                  OpenapiResponse(Fixed(StatusCode.BadRequest), "Invalid ID supplied", List()),
                  OpenapiResponse(Fixed(StatusCode.NotFound), "Pet not found", List()),
                  OpenapiResponse(Fixed(StatusCode.MethodNotAllowed), "Validation exception", List())
                ),
                Some(
                  OpenapiRequestBody(
                    Some(true),
                    Some("Update an existent pet in the store"),
                    List(
                      OpenapiRequestBodyContent(MediaType.ApplicationJson, OpenapiSchemaRef("#/components/schemas/Pet"))
                    )
                  )
                ),
                Some("Update an existing pet")
              ),
              OpenapiPathMethod(
                Method.POST,
                List(),
                List(
                  OpenapiResponse(
                    Fixed(StatusCode.Ok),
                    "Successful operation",
                    List(
                      OpenapiResponseContent(MediaType.ApplicationJson, OpenapiSchemaRef("#/components/schemas/Pet"))
                    )
                  ),
                  OpenapiResponse(Fixed(StatusCode.MethodNotAllowed), "Invalid input", List())
                ),
                Some(
                  OpenapiRequestBody(
                    Some(true),
                    Some("Create a new pet in the store"),
                    List(
                      OpenapiRequestBodyContent(
                        MediaType.ApplicationJson,
                        OpenapiSchemaRef("#/components/schemas/Pet")
                      ),
                      OpenapiRequestBodyContent(
                        MediaType.ApplicationXWwwFormUrlencoded,
                        OpenapiSchemaRef("#/components/schemas/Pet")
                      )
                    )
                  )
                ),
                Some("Add a new pet to the store")
              )
            )
          ),
          OpenapiPath(
            "/store/order/{orderId}",
            List(
              OpenapiPathMethod(
                Method.GET,
                List(
                  OpenapiParameter(
                    "orderId",
                    OpenapiParamType.Path,
                    Some(true),
                    Some("ID of order that needs to be fetched"),
                    OpenapiSchemaLong(nullable = false, None)
                  )
                ),
                List(
                  OpenapiResponse(
                    Fixed(StatusCode.Ok),
                    "successful operation",
                    List(
                      OpenapiResponseContent(MediaType.ApplicationJson, OpenapiSchemaRef("#/components/schemas/Order"))
                    )
                  ),
                  OpenapiResponse(Fixed(StatusCode.BadRequest), "Invalid ID supplied", List()),
                  OpenapiResponse(Fixed(StatusCode.NotFound), "Order not found", List())
                ),
                None,
                Some("Find purchase order by ID")
              ),
              OpenapiPathMethod(
                Method.DELETE,
                List(
                  OpenapiParameter(
                    "orderId",
                    OpenapiParamType.Path,
                    Some(true),
                    Some("ID of the order that needs to be deleted"),
                    OpenapiSchemaLong(nullable = false, None)
                  )
                ),
                List(
                  OpenapiResponse(Fixed(StatusCode.BadRequest), "Invalid ID supplied", List()),
                  OpenapiResponse(Fixed(StatusCode.NotFound), "Order not found", List())
                ),
                None,
                Some("Delete purchase order by ID")
              )
            )
          ),
          OpenapiPath(
            "/user/logout",
            List(OpenapiPathMethod(Method.GET, List(), List(), None, Some("Logs out current logged in user session")))
          ),
          OpenapiPath(
            "/store/inventory",
            List(
              OpenapiPathMethod(
                Method.GET,
                List(),
                List(
                  OpenapiResponse(
                    Fixed(StatusCode.Ok),
                    "successful operation",
                    List(
                      OpenapiResponseContent(
                        MediaType.ApplicationJson,
                        OpenapiSchemaObject(Map(), List(), nullable = false, None, Left(false))
                      )
                    )
                  )
                ),
                None,
                Some("Returns pet inventories by status")
              )
            )
          ),
          OpenapiPath(
            "/user/createWithList",
            List(
              OpenapiPathMethod(
                Method.POST,
                List(),
                List(
                  OpenapiResponse(
                    Fixed(StatusCode.Ok),
                    "Successful operation",
                    List(
                      OpenapiResponseContent(
                        MediaType.ApplicationJson,
                        OpenapiSchemaRef("#/components/schemas/User")
                      )
                    )
                  )
                ),
                Some(
                  OpenapiRequestBody(
                    None,
                    None,
                    List(
                      OpenapiRequestBodyContent(
                        MediaType.ApplicationJson,
                        OpenapiSchemaArray(OpenapiSchemaRef("#/components/schemas/User"), nullable = false)
                      )
                    )
                  )
                ),
                Some("Creates list of users with given input array")
              )
            )
          ),
          OpenapiPath(
            "/user",
            List(
              OpenapiPathMethod(
                Method.POST,
                List(),
                List(),
                Some(
                  OpenapiRequestBody(
                    None,
                    Some("Created user object"),
                    List(
                      OpenapiRequestBodyContent(
                        MediaType.ApplicationJson,
                        OpenapiSchemaRef("#/components/schemas/User")
                      )
                    )
                  )
                ),
                Some("Create user")
              )
            )
          ),
          OpenapiPath(
            "/pet/findByStatus",
            List(
              OpenapiPathMethod(
                Method.GET,
                List(
                  OpenapiParameter(
                    "status",
                    OpenapiParamType.Query,
                    Some(false),
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
          ),
          OpenapiPath(
            "/user/{username}",
            List(
              OpenapiPathMethod(
                Method.GET,
                List(
                  OpenapiParameter(
                    "username",
                    OpenapiParamType.Path,
                    Some(true),
                    Some("The name that needs to be fetched. Use user1 for testing. "),
                    OpenapiSchemaString(false, None)
                  )
                ),
                List(
                  OpenapiResponse(
                    Fixed(StatusCode.Ok),
                    "successful operation",
                    List(
                      OpenapiResponseContent(MediaType.ApplicationJson, OpenapiSchemaRef("#/components/schemas/User"))
                    )
                  ),
                  OpenapiResponse(Fixed(StatusCode.BadRequest), "Invalid username supplied", List()),
                  OpenapiResponse(Fixed(StatusCode.NotFound), "User not found", List())
                ),
                None,
                Some("Get user by user name")
              ),
              OpenapiPathMethod(
                Method.PUT,
                List(
                  OpenapiParameter(
                    "username",
                    OpenapiParamType.Path,
                    Some(true),
                    Some("name that need to be deleted"),
                    OpenapiSchemaString(false, None)
                  )
                ),
                List(),
                Some(
                  OpenapiRequestBody(
                    None,
                    Some("Update an existent user in the store"),
                    List(
                      OpenapiRequestBodyContent(
                        MediaType.ApplicationJson,
                        OpenapiSchemaRef("#/components/schemas/User")
                      )
                    )
                  )
                ),
                Some("Update user")
              ),
              OpenapiPathMethod(
                Method.DELETE,
                List(
                  OpenapiParameter(
                    "username",
                    OpenapiParamType.Path,
                    Some(true),
                    Some("The name that needs to be deleted"),
                    OpenapiSchemaString(false, None)
                  )
                ),
                List(
                  OpenapiResponse(Fixed(StatusCode.BadRequest), "Invalid username supplied", List()),
                  OpenapiResponse(Fixed(StatusCode.NotFound), "User not found", List())
                ),
                None,
                Some("Delete user")
              )
            )
          ),
          OpenapiPath(
            "/user/login",
            List(
              OpenapiPathMethod(
                Method.GET,
                List(
                  OpenapiParameter(
                    "username",
                    OpenapiParamType.Query,
                    Some(false),
                    Some("The user name for login"),
                    OpenapiSchemaString(false, None)
                  ),
                  OpenapiParameter(
                    "password",
                    OpenapiParamType.Query,
                    Some(false),
                    Some("The password for login in clear text"),
                    OpenapiSchemaString(false, None)
                  )
                ),
                List(
                  OpenapiResponse(
                    Fixed(StatusCode.Ok),
                    "successful operation",
                    List(OpenapiResponseContent(MediaType.ApplicationJson, OpenapiSchemaString(false, None)))
                  ),
                  OpenapiResponse(Fixed(StatusCode.BadRequest), "Invalid username/password supplied", List())
                ),
                None,
                Some("Logs user into the system")
              )
            )
          ),
          OpenapiPath(
            "/pet/{petId}",
            List(
              OpenapiPathMethod(
                Method.GET,
                List(
                  OpenapiParameter(
                    "petId",
                    OpenapiParamType.Path,
                    Some(true),
                    Some("ID of pet to return"),
                    OpenapiSchemaLong(false, None)
                  )
                ),
                List(
                  OpenapiResponse(
                    Fixed(StatusCode.Ok),
                    "successful operation",
                    List(
                      OpenapiResponseContent(MediaType.ApplicationJson, OpenapiSchemaRef("#/components/schemas/Pet"))
                    )
                  ),
                  OpenapiResponse(Fixed(StatusCode.BadRequest), "Invalid ID supplied", List()),
                  OpenapiResponse(Fixed(StatusCode.NotFound), "Pet not found", List())
                ),
                None,
                Some("Find pet by ID")
              ),
              OpenapiPathMethod(
                Method.POST,
                List(
                  OpenapiParameter(
                    "petId",
                    OpenapiParamType.Path,
                    Some(true),
                    Some("ID of pet that needs to be updated"),
                    OpenapiSchemaLong(false, None)
                  ),
                  OpenapiParameter(
                    "name",
                    OpenapiParamType.Query,
                    None,
                    Some("Name of pet that needs to be updated"),
                    OpenapiSchemaString(false, None)
                  ),
                  OpenapiParameter(
                    "status",
                    OpenapiParamType.Query,
                    None,
                    Some("Status of pet that needs to be updated"),
                    OpenapiSchemaString(false, None)
                  )
                ),
                List(OpenapiResponse(Fixed(StatusCode.MethodNotAllowed), "Invalid input", List())),
                None,
                Some("Updates a pet in the store with form data")
              ),
              OpenapiPathMethod(
                Method.DELETE,
                List(
                  OpenapiParameter(
                    "api_key",
                    OpenapiParamType.Header,
                    Some(false),
                    Some(""),
                    OpenapiSchemaString(false, None)
                  ),
                  OpenapiParameter(
                    "petId",
                    OpenapiParamType.Path,
                    Some(true),
                    Some("Pet id to delete"),
                    OpenapiSchemaLong(false, None)
                  )
                ),
                List(OpenapiResponse(Fixed(StatusCode.BadRequest), "Invalid pet value", List())),
                None,
                Some("Deletes a pet")
              )
            )
          ),
          OpenapiPath(
            "/pet/{petId}/uploadImage",
            List(
              OpenapiPathMethod(
                Method.POST,
                List(
                  OpenapiParameter(
                    "petId",
                    OpenapiParamType.Path,
                    Some(true),
                    Some("ID of pet to update"),
                    OpenapiSchemaLong(false, None)
                  ),
                  OpenapiParameter(
                    "additionalMetadata",
                    OpenapiParamType.Query,
                    Some(false),
                    Some("Additional Metadata"),
                    OpenapiSchemaString(false, None)
                  )
                ),
                List(
                  OpenapiResponse(
                    Fixed(StatusCode.Ok),
                    "successful operation",
                    List(
                      OpenapiResponseContent(
                        MediaType.ApplicationJson,
                        OpenapiSchemaRef("#/components/schemas/ApiResponse")
                      )
                    )
                  )
                ),
                Some(
                  OpenapiRequestBody(
                    None,
                    None,
                    List(OpenapiRequestBodyContent(MediaType.ApplicationOctetStream, OpenapiSchemaBinary(false)))
                  )
                ),
                Some("uploads an image")
              )
            )
          )
        ),
        OpenapiComponent(
          Map(
            "User" -> OpenapiSchemaObject(
              properties = Map(
                "email" -> OpenapiSchemaString(nullable = false, Some("john@email.com")),
                "username" -> OpenapiSchemaString(nullable = false, Some("theUser")),
                "userStatus" -> OpenapiSchemaInt(nullable = false, None),
                "lastName" -> OpenapiSchemaString(nullable = false, example = Some("James")),
                "firstName" -> OpenapiSchemaString(nullable = false, Some("John")),
                "id" -> OpenapiSchemaLong(nullable = false, None),
                "phone" -> OpenapiSchemaString(nullable = false, Some("12345")),
                "password" -> OpenapiSchemaString(nullable = false, Some("12345"))
              ),
              required = Seq.empty,
              nullable = false,
              example = None,
              additionalProperties = Left(false)
            ),
            "ApiResponse" -> OpenapiSchemaObject(
              properties = Map(
                "code" -> OpenapiSchemaInt(nullable = false, example = None),
                "type" -> OpenapiSchemaString(nullable = false, example = None),
                "message" -> OpenapiSchemaString(nullable = false, example = None)
              ),
              required = Seq.empty,
              nullable = false,
              example = None,
              additionalProperties = Left(false)
            ),
            "Address" -> OpenapiSchemaObject(
              properties = Map(
                "street" -> OpenapiSchemaString(nullable = false, Some("437 Lytton")),
                "city" -> OpenapiSchemaString(nullable = false, Some("Palo Alto")),
                "state" -> OpenapiSchemaString(nullable = false, Some("CA")),
                "zip" -> OpenapiSchemaString(nullable = false, Some("94301"))
              ),
              required = Seq.empty,
              nullable = false,
              example = None,
              additionalProperties = Left(false)
            ),
            "Tag" -> OpenapiSchemaObject(
              properties = Map(
                "id" -> OpenapiSchemaLong(nullable = false, None),
                "name" -> OpenapiSchemaString(nullable = false, Some("tag1"))
              ),
              required = Seq.empty,
              nullable = false,
              example = None,
              additionalProperties = Left(false)
            ),
            "Pet" -> OpenapiSchemaObject(
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
            ),
            "Order" -> OpenapiSchemaObject(
              properties = Map(
                "shipDate" -> OpenapiSchemaDateTime(nullable = false, None),
                "quantity" -> OpenapiSchemaInt(nullable = false, None),
                "complete" -> OpenapiSchemaBoolean(nullable = false, None),
                "status" -> OpenapiSchemaString(nullable = false, Some("approved")),
                "petId" -> OpenapiSchemaLong(nullable = false, None),
                "id" -> OpenapiSchemaLong(nullable = false, None)
              ),
              required = List.empty,
              nullable = false,
              example = None,
              additionalProperties = Left(false)
            ),
            "Category" -> OpenapiSchemaObject(
              Map(
                "id" -> OpenapiSchemaLong(nullable = false, None),
                "name" -> OpenapiSchemaString(nullable = false, Some("Dogs"))
              ),
              List.empty,
              nullable = false,
              example = None,
              additionalProperties = Left(false)
            ),
            "Customer" -> OpenapiSchemaObject(
              properties = Map(
                "id" -> OpenapiSchemaLong(nullable = false, None),
                "username" -> OpenapiSchemaString(nullable = false, Some("fehguy")),
                "address" -> OpenapiSchemaArray(OpenapiSchemaRef("#/components/schemas/Address"), nullable = false)
              ),
              required = List(),
              nullable = false,
              example = None,
              additionalProperties = Left(false)
            )
          )
        )
      )
    )
  }
}
