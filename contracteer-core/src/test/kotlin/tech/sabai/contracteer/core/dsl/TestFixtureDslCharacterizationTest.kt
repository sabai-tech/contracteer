package tech.sabai.contracteer.core.dsl

import tech.sabai.contracteer.core.TestFixture.allOfDataType
import tech.sabai.contracteer.core.TestFixture.arrayDataType
import tech.sabai.contracteer.core.TestFixture.booleanDataType
import tech.sabai.contracteer.core.TestFixture.integerDataType
import tech.sabai.contracteer.core.TestFixture.objectDataType
import tech.sabai.contracteer.core.TestFixture.oneOfDataType
import tech.sabai.contracteer.core.TestFixture.stringDataType
import tech.sabai.contracteer.core.codec.ContentCodec
import tech.sabai.contracteer.core.codec.DeepObjectParameterCodec
import tech.sabai.contracteer.core.codec.FormParameterCodec
import tech.sabai.contracteer.core.codec.LabelParameterCodec
import tech.sabai.contracteer.core.codec.MatrixParameterCodec
import tech.sabai.contracteer.core.codec.PipeDelimitedParameterCodec
import tech.sabai.contracteer.core.codec.SimpleParameterCodec
import tech.sabai.contracteer.core.codec.SpaceDelimitedParameterCodec
import tech.sabai.contracteer.core.datatype.Discriminator
import tech.sabai.contracteer.core.operation.ApiOperation
import tech.sabai.contracteer.core.operation.BodySchema
import tech.sabai.contracteer.core.operation.ContentType
import tech.sabai.contracteer.core.operation.ParameterElement
import tech.sabai.contracteer.core.operation.ParameterSchema
import tech.sabai.contracteer.core.operation.RequestSchema
import tech.sabai.contracteer.core.operation.ResponseSchema
import tech.sabai.contracteer.core.operation.ResponseSchemas
import tech.sabai.contracteer.core.operation.Scenario
import tech.sabai.contracteer.core.operation.ScenarioBody
import tech.sabai.contracteer.core.operation.ScenarioRequest
import tech.sabai.contracteer.core.operation.ScenarioResponse
import tech.sabai.contracteer.core.serde.JsonSerde
import tech.sabai.contracteer.core.serde.PlainTextSerde
import kotlin.test.Test

class TestFixtureDslCharacterizationTest {

  @Test
  fun `minimal operation with path param and json response`() {
    // Given
    val idType = integerDataType()
    val userType = objectDataType(properties = mapOf("id" to idType, "name" to stringDataType()))
    val expected = ApiOperation(
      path = "/users/{id}",
      method = "GET",
      requestSchema = RequestSchema(
        parameters = listOf(ParameterSchema(ParameterElement.PathParam("id"), idType, true, SimpleParameterCodec("id", false))),
        bodies = emptyList()
      ),
      responseSchemas = ResponseSchemas(byStatusCode = mapOf(
        200 to ResponseSchema(emptyList(), listOf(BodySchema(ContentType("application/json"), userType, true, JsonSerde)))
      )),
      scenarios = emptyList()
    )

    // When
    val actual = apiOperation("GET", "/users/{id}") {
      request { pathParam("id", idType) }
      response(200) { jsonBody(userType) }
    }

    // Then
    assert(expected == actual)
  }

  @Test
  fun `query param with default form codec`() {
    val pageType = integerDataType()
    val expected = paramOnlyOperation(
      ParameterSchema(ParameterElement.QueryParam("page"), pageType, false, FormParameterCodec("page", true))
    )

    val actual = apiOperation("GET", "/items") {
      request { queryParam("page", pageType) }
    }

    assert(expected == actual)
  }

  @Test
  fun `query param with pipeDelimited codec`() {
    val tagsType = arrayDataType(stringDataType())
    val expected = paramOnlyOperation(
      ParameterSchema(ParameterElement.QueryParam("tags"), tagsType, false, PipeDelimitedParameterCodec("tags"))
    )

    val actual = apiOperation("GET", "/items") {
      request { queryParam("tags", tagsType, codec = pipeDelimited()) }
    }

    assert(expected == actual)
  }

  @Test
  fun `query param with spaceDelimited codec`() {
    val tagsType = arrayDataType(stringDataType())
    val expected = paramOnlyOperation(
      ParameterSchema(ParameterElement.QueryParam("tags"), tagsType, false, SpaceDelimitedParameterCodec("tags"))
    )

    val actual = apiOperation("GET", "/items") {
      request { queryParam("tags", tagsType, codec = spaceDelimited()) }
    }

    assert(expected == actual)
  }

  @Test
  fun `query param with deepObject codec`() {
    val filterType = objectDataType(properties = mapOf("color" to stringDataType()))
    val expected = paramOnlyOperation(
      ParameterSchema(ParameterElement.QueryParam("filter"), filterType, false, DeepObjectParameterCodec("filter"))
    )

    val actual = apiOperation("GET", "/items") {
      request { queryParam("filter", filterType, codec = deepObject()) }
    }

    assert(expected == actual)
  }

  @Test
  fun `query param with content codec`() {
    val filterType = objectDataType(properties = mapOf("color" to stringDataType()))
    val expected = paramOnlyOperation(
      ParameterSchema(ParameterElement.QueryParam("filter"), filterType, false, ContentCodec("filter", JsonSerde))
    )

    val actual = apiOperation("GET", "/items") {
      request { queryParam("filter", filterType, codec = content(JsonSerde)) }
    }

    assert(expected == actual)
  }

  @Test
  fun `path param with matrix codec`() {
    val idType = integerDataType()
    val expected = paramOnlyOperation(
      ParameterSchema(ParameterElement.PathParam("id"), idType, true, MatrixParameterCodec("id", true))
    ).copy(path = "/users;id={id}")

    val actual = apiOperation("GET", "/users;id={id}") {
      request { pathParam("id", idType, codec = matrix(explode = true)) }
    }

    assert(expected == actual)
  }

  @Test
  fun `path param with label codec`() {
    val idType = integerDataType()
    val expected = paramOnlyOperation(
      ParameterSchema(ParameterElement.PathParam("id"), idType, true, LabelParameterCodec("id", false))
    ).copy(path = "/users.{id}")

    val actual = apiOperation("GET", "/users.{id}") {
      request { pathParam("id", idType, codec = label()) }
    }

    assert(expected == actual)
  }

  @Test
  fun `header and cookie with default codecs`() {
    val headerType = stringDataType()
    val cookieType = stringDataType()
    val expected = ApiOperation(
      path = "/x", method = "GET",
      requestSchema = RequestSchema(
        parameters = listOf(
          ParameterSchema(ParameterElement.Header("X-Trace-Id"), headerType, false, SimpleParameterCodec("X-Trace-Id", false)),
          ParameterSchema(ParameterElement.Cookie("session"), cookieType, false, FormParameterCodec("session", false))
        ),
        bodies = emptyList()
      ),
      responseSchemas = ResponseSchemas(),
      scenarios = emptyList()
    )

    val actual = apiOperation("GET", "/x") {
      request {
        header("X-Trace-Id", headerType)
        cookie("session", cookieType)
      }
    }

    assert(expected == actual)
  }

  @Test
  fun `request bodies json plainText and generic`() {
    val jsonType = objectDataType(properties = mapOf("a" to stringDataType()))
    val plainType = stringDataType()
    val xmlType = stringDataType()
    val expected = ApiOperation(
      path = "/x", method = "POST",
      requestSchema = RequestSchema(
        parameters = emptyList(),
        bodies = listOf(
          BodySchema(ContentType("application/json"), jsonType, true, JsonSerde),
          BodySchema(ContentType("text/plain"), plainType, true, PlainTextSerde),
          BodySchema(ContentType("application/xml"), xmlType, false, JsonSerde)
        )
      ),
      responseSchemas = ResponseSchemas(),
      scenarios = emptyList()
    )

    val actual = apiOperation("POST", "/x") {
      request {
        jsonBody(jsonType)
        plainTextBody(plainType)
        body("application/xml", xmlType, JsonSerde, isRequired = false)
      }
    }

    assert(expected == actual)
  }

  @Test
  fun `multiple status codes with multiple content types`() {
    val reqJson = objectDataType(properties = mapOf("q" to stringDataType()))
    val reqXml = stringDataType()
    val resJson = objectDataType(properties = mapOf("items" to stringDataType()))
    val resXml = stringDataType()
    val expected = ApiOperation(
      path = "/data", method = "POST",
      requestSchema = RequestSchema(
        parameters = emptyList(),
        bodies = listOf(
          BodySchema(ContentType("application/json"), reqJson, true, JsonSerde),
          BodySchema(ContentType("application/xml"), reqXml, true, JsonSerde)
        )
      ),
      responseSchemas = ResponseSchemas(byStatusCode = mapOf(
        200 to ResponseSchema(emptyList(), listOf(
          BodySchema(ContentType("application/json"), resJson, true, JsonSerde),
          BodySchema(ContentType("application/xml"), resXml, true, JsonSerde)
        )),
        404 to ResponseSchema(emptyList(), emptyList())
      )),
      scenarios = emptyList()
    )

    val actual = apiOperation("POST", "/data") {
      request {
        jsonBody(reqJson)
        body("application/xml", reqXml, JsonSerde)
      }
      response(200) {
        jsonBody(resJson)
        body("application/xml", resXml, JsonSerde)
      }
      response(404) {}
    }

    assert(expected == actual)
  }

  @Test
  fun `scenarios with and without bodies`() {
    val idType = integerDataType()
    val userType = objectDataType(properties = mapOf("id" to idType, "name" to stringDataType()))
    val expected = ApiOperation(
      path = "/users/{id}", method = "GET",
      requestSchema = RequestSchema(
        parameters = listOf(ParameterSchema(ParameterElement.PathParam("id"), idType, true, SimpleParameterCodec("id", false))),
        bodies = emptyList()
      ),
      responseSchemas = ResponseSchemas(byStatusCode = mapOf(
        200 to ResponseSchema(emptyList(), listOf(BodySchema(ContentType("application/json"), userType, true, JsonSerde))),
        404 to ResponseSchema(emptyList(), emptyList())
      )),
      scenarios = listOf(
        Scenario(
          path = "/users/{id}", method = "GET", key = "validUser", statusCode = 200,
          request = ScenarioRequest(mapOf(ParameterElement.PathParam("id") to 1), null),
          response = ScenarioResponse(emptyMap(), ScenarioBody(ContentType("application/json"), mapOf("id" to 1, "name" to "John")))
        ),
        Scenario(
          path = "/users/{id}", method = "GET", key = "notFound", statusCode = 404,
          request = ScenarioRequest(mapOf(ParameterElement.PathParam("id") to 999), null),
          response = ScenarioResponse(emptyMap(), null)
        )
      )
    )

    val actual = apiOperation("GET", "/users/{id}") {
      request { pathParam("id", idType) }
      response(200) { jsonBody(userType) }
      response(404) {}
      scenario("validUser", status = 200) {
        request { pathParam["id"] = 1 }
        response { jsonBody(json { "id" to 1; "name" to "John" }) }
      }
      scenario("notFound", status = 404) {
        request { pathParam["id"] = 999 }
      }
    }

    assertOperationEquals(expected, actual)
  }

  @Test
  fun `datatype composition with allOf and oneOf and discriminator`() {
    val pet = objectDataType(name = "pet", properties = mapOf("petType" to stringDataType()), requiredProperties = setOf("petType"))
    val catBase = objectDataType(name = "catBase", properties = mapOf("hunts" to booleanDataType(), "age" to integerDataType()), requiredProperties = setOf("hunts", "age"))
    val cat = allOfDataType(name = "cat", subTypes = listOf(pet, catBase), discriminator = Discriminator("petType", mapOf("CAT" to "cat")))
    val dogBase = objectDataType(name = "dogBase", properties = mapOf("bark" to booleanDataType()))
    val dog = allOfDataType(name = "dog", subTypes = listOf(pet, dogBase), discriminator = Discriminator("petType", mapOf("DOG" to "dog")))
    val animal = oneOfDataType(name = "animal", subTypes = listOf(cat, dog), discriminator = Discriminator("petType", mapOf("CAT" to "cat", "DOG" to "dog")))

    val expected = paramOnlyOperation(
      ParameterSchema(ParameterElement.QueryParam("animal"), animal, false, FormParameterCodec("animal", true))
    ).copy(path = "/zoo")

    val actual = apiOperation("GET", "/zoo") {
      request { queryParam("animal", animal) }
    }

    assert(expected == actual)
  }

  @Test
  fun `scenario with query and header and cookie values`() {
    val qType = stringDataType()
    val hType = stringDataType()
    val cType = stringDataType()
    val expected = ApiOperation(
      path = "/search", method = "GET",
      requestSchema = RequestSchema(
        parameters = listOf(
          ParameterSchema(ParameterElement.QueryParam("q"), qType, false, FormParameterCodec("q", true)),
          ParameterSchema(ParameterElement.Header("X-Trace"), hType, false, SimpleParameterCodec("X-Trace", false)),
          ParameterSchema(ParameterElement.Cookie("session"), cType, false, FormParameterCodec("session", false))
        ),
        bodies = emptyList()
      ),
      responseSchemas = ResponseSchemas(byStatusCode = mapOf(
        200 to ResponseSchema(emptyList(), emptyList())
      )),
      scenarios = listOf(
        Scenario(
          path = "/search", method = "GET", key = "basic", statusCode = 200,
          request = ScenarioRequest(
            mapOf(
              ParameterElement.QueryParam("q") to "hello",
              ParameterElement.Header("X-Trace") to "abc",
              ParameterElement.Cookie("session") to "xyz"
            ),
            null
          ),
          response = ScenarioResponse(mapOf(ParameterElement.Header("X-Trace") to "abc"), null)
        )
      )
    )

    val actual = apiOperation("GET", "/search") {
      request {
        queryParam("q", qType)
        header("X-Trace", hType)
        cookie("session", cType)
      }
      response(200) {}
      scenario("basic", status = 200) {
        request {
          queryParam["q"] = "hello"
          header["X-Trace"] = "abc"
          cookie["session"] = "xyz"
        }
        response { header["X-Trace"] = "abc" }
      }
    }

    assertOperationEquals(expected, actual)
  }

  // --- helpers -----------------------------------------------------------

  private fun paramOnlyOperation(parameter: ParameterSchema): ApiOperation = ApiOperation(
    path = "/items", method = "GET",
    requestSchema = RequestSchema(listOf(parameter), emptyList()),
    responseSchemas = ResponseSchemas(),
    scenarios = emptyList()
  )

  // ScenarioRequest/Response/Body are not data classes, so structural equality falls through
  // to reference equality. Compare those fields explicitly.
  private fun assertOperationEquals(expected: ApiOperation, actual: ApiOperation) {
    assert(expected.copy(scenarios = emptyList()) == actual.copy(scenarios = emptyList()))
    assert(expected.scenarios.size == actual.scenarios.size)
    expected.scenarios.zip(actual.scenarios).forEach { (e, a) -> assertScenarioEquals(e, a) }
  }

  private fun assertScenarioEquals(expected: Scenario, actual: Scenario) {
    assert(expected.path == actual.path)
    assert(expected.method == actual.method)
    assert(expected.key == actual.key)
    assert(expected.statusCode == actual.statusCode)
    assert(expected.request.parameterValues == actual.request.parameterValues)
    assertBodyEquals(expected.request.body, actual.request.body)
    assert(expected.response.headers == actual.response.headers)
    assertBodyEquals(expected.response.body, actual.response.body)
  }

  private fun assertBodyEquals(expected: ScenarioBody?, actual: ScenarioBody?) {
    assert((expected == null) == (actual == null))
    if (expected == null || actual == null) return
    assert(expected.contentType == actual.contentType)
    assert(expected.value == actual.value)
  }
}
