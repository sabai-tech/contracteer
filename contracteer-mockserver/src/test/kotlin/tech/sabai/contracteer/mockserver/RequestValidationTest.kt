package tech.sabai.contracteer.mockserver

import io.restassured.RestAssured
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.jupiter.api.AfterEach
import tech.sabai.contracteer.core.codec.ContentCodec
import tech.sabai.contracteer.core.codec.FormParameterCodec
import tech.sabai.contracteer.core.operation.ContentType
import tech.sabai.contracteer.core.operation.ParameterElement.*
import tech.sabai.contracteer.core.operation.ParameterSchema
import tech.sabai.contracteer.core.serde.JsonSerde
import tech.sabai.contracteer.mockserver.TestFixture.apiOperation
import tech.sabai.contracteer.mockserver.TestFixture.bodySchema
import tech.sabai.contracteer.core.TestFixture.integerDataType
import tech.sabai.contracteer.core.TestFixture.objectDataType
import tech.sabai.contracteer.core.TestFixture.oneOfDataType
import tech.sabai.contracteer.core.TestFixture.stringDataType
import tech.sabai.contracteer.mockserver.TestFixture.parameterSchema
import tech.sabai.contracteer.mockserver.TestFixture.requestSchema
import tech.sabai.contracteer.mockserver.TestFixture.responseSchema
import kotlin.test.Test

class RequestValidationTest {

  private lateinit var mockServer: MockServer

  @AfterEach
  fun tearDown() {
    mockServer.stop()
  }

  // --- Path parameters ---

  @Test
  fun `responds successfully when path parameter matches schema type`() {
    // Given
    val operation = apiOperation(
      path = "/v1/users/{id}",
      method = "GET",
      requestSchema = requestSchema(
        parameters = listOf(parameterSchema(PathParam("id"), integerDataType()))
      ),
      responses = mapOf(
        200 to responseSchema(
          bodies = listOf(bodySchema(
            contentType = ContentType("application/json"),
            dataType = objectDataType(properties = mapOf("id" to integerDataType()))))
        )
      )
    )
    mockServer = MockServer(listOf(operation))
    mockServer.start()
    RestAssured.port = mockServer.port()

    // When / Then
    given()
      .accept("application/json")
      .get("/v1/users/123")
      .then()
      .assertThat()
      .statusCode(200)
      .body("id", notNullValue())
  }

  @Test
  fun `responds with 418 when path parameter does not match schema type`() {
    // Given
    val operation = apiOperation(
      path = "/v1/users/{id}",
      method = "GET",
      requestSchema = requestSchema(
        parameters = listOf(parameterSchema(PathParam("id"), integerDataType()))
      ),
      responses = mapOf(
        200 to responseSchema(
          bodies = listOf(bodySchema(
            contentType = ContentType("application/json"),
            dataType = objectDataType(properties = mapOf("id" to integerDataType()))))
        )
      )
    )
    mockServer = MockServer(listOf(operation))
    mockServer.start()
    RestAssured.port = mockServer.port()

    // When / Then
    given()
      .accept("application/json")
      .get("/v1/users/abc")
      .then()
      .assertThat()
      .statusCode(418)
  }

  // --- Query parameters ---

  @Test
  fun `responds successfully when query parameter matches multiple oneOf variants`() {
    // Given
    val variantA = objectDataType(properties = mapOf("name" to stringDataType()))
    val variantB = objectDataType(properties = mapOf("name" to stringDataType(), "age" to integerDataType()))
    val operation = apiOperation(
      path = "/v1/users",
      method = "GET",
      requestSchema = requestSchema(
        parameters = listOf(
          ParameterSchema(
            QueryParam("filter"),
            oneOfDataType(subTypes = listOf(variantA, variantB)),
            isRequired = true,
            ContentCodec("filter", JsonSerde)
          )
        )
      ),
      responses = mapOf(
        200 to responseSchema(
          bodies = listOf(bodySchema(
            contentType = ContentType("application/json"),
            dataType = objectDataType(properties = mapOf("id" to integerDataType()))))
        )
      )
    )
    mockServer = MockServer(listOf(operation))
    mockServer.start()
    RestAssured.port = mockServer.port()

    // When / Then
    given()
      .accept("application/json")
      .queryParam("filter", """{"name":"john"}""")
      .get("/v1/users")
      .then()
      .assertThat()
      .statusCode(200)
  }

  @Test
  fun `responds successfully when required query parameter is present and matches schema type`() {
    // Given
    val operation = apiOperation(
      path = "/v1/users",
      method = "GET",
      requestSchema = requestSchema(
        parameters = listOf(parameterSchema(QueryParam("id"), integerDataType(), isRequired = true))
      ),
      responses = mapOf(
        200 to responseSchema(
          bodies = listOf(bodySchema(
            contentType = ContentType("application/json"),
            dataType = objectDataType(properties = mapOf("id" to integerDataType()))))
        )
      )
    )
    mockServer = MockServer(listOf(operation))
    mockServer.start()
    RestAssured.port = mockServer.port()

    // When / Then
    given()
      .accept("application/json")
      .get("/v1/users?id=123")
      .then()
      .assertThat()
      .statusCode(200)
      .body("id", notNullValue())
  }

  @Test
  fun `responds with 418 when required query parameter is missing`() {
    // Given
    val operation = apiOperation(
      path = "/v1/users",
      method = "GET",
      requestSchema = requestSchema(
        parameters = listOf(parameterSchema(QueryParam("id"), integerDataType(), isRequired = true))
      ),
      responses = mapOf(
        200 to responseSchema(
          bodies = listOf(bodySchema(
            contentType = ContentType("application/json"),
            dataType = objectDataType(properties = mapOf("id" to integerDataType()))))
        )
      )
    )
    mockServer = MockServer(listOf(operation))
    mockServer.start()
    RestAssured.port = mockServer.port()

    // When / Then
    given()
      .accept("application/json")
      .get("/v1/users")
      .then()
      .assertThat()
      .statusCode(418)
  }

  @Test
  fun `responds successfully when optional query parameter is absent`() {
    // Given
    val operation = apiOperation(
      path = "/v1/users",
      method = "GET",
      requestSchema = requestSchema(
        parameters = listOf(parameterSchema(QueryParam("id"), integerDataType(), isRequired = false))
      ),
      responses = mapOf(
        200 to responseSchema(
          bodies = listOf(bodySchema(
            contentType = ContentType("application/json"),
            dataType = objectDataType(properties = mapOf("id" to integerDataType()))))
        )
      )
    )
    mockServer = MockServer(listOf(operation))
    mockServer.start()
    RestAssured.port = mockServer.port()

    // When / Then
    given()
      .accept("application/json")
      .get("/v1/users")
      .then()
      .assertThat()
      .statusCode(200)
      .body("id", notNullValue())
  }

  @Test
  fun `responds successfully when query parameter with allowReserved contains reserved characters`() {
    // Given
    val operation = apiOperation(
      path = "/v1/search",
      method = "GET",
      requestSchema = requestSchema(
        parameters = listOf(
          ParameterSchema(
            QueryParam("path"),
            stringDataType(),
            isRequired = true,
            FormParameterCodec("path", explode = true, allowReserved = true)
          )
        )
      ),
      responses = mapOf(
        200 to responseSchema(
          bodies = listOf(bodySchema(
            contentType = ContentType("application/json"),
            dataType = objectDataType(properties = mapOf("id" to integerDataType()))))
        )
      )
    )
    mockServer = MockServer(listOf(operation))
    mockServer.start()
    RestAssured.port = mockServer.port()

    // When / Then
    given()
      .accept("application/json")
      .get("/v1/search?path=/users/123")
      .then()
      .assertThat()
      .statusCode(200)
      .body("id", notNullValue())
  }

  // --- Header parameters ---

  @Test
  fun `responds successfully when required header parameter is present and matches schema type`() {
    // Given
    val operation = apiOperation(
      path = "/v1/users",
      method = "GET",
      requestSchema = requestSchema(
        parameters = listOf(parameterSchema(Header("X-Request-Id"), integerDataType(), isRequired = true))
      ),
      responses = mapOf(
        200 to responseSchema(
          bodies = listOf(bodySchema(
            contentType = ContentType("application/json"),
            dataType = objectDataType(properties = mapOf("id" to integerDataType()))))
        )
      )
    )
    mockServer = MockServer(listOf(operation))
    mockServer.start()
    RestAssured.port = mockServer.port()

    // When / Then
    given()
      .accept("application/json")
      .header("X-Request-Id", "123")
      .get("/v1/users")
      .then()
      .assertThat()
      .statusCode(200)
      .body("id", notNullValue())
  }

  @Test
  fun `responds with 418 when required header parameter is missing`() {
    // Given
    val operation = apiOperation(
      path = "/v1/users",
      method = "GET",
      requestSchema = requestSchema(
        parameters = listOf(parameterSchema(Header("X-Request-Id"), integerDataType(), isRequired = true))
      ),
      responses = mapOf(
        200 to responseSchema(
          bodies = listOf(bodySchema(
            contentType = ContentType("application/json"),
            dataType = objectDataType(properties = mapOf("id" to integerDataType()))))
        )
      )
    )
    mockServer = MockServer(listOf(operation))
    mockServer.start()
    RestAssured.port = mockServer.port()

    // When / Then
    given()
      .accept("application/json")
      .get("/v1/users")
      .then()
      .assertThat()
      .statusCode(418)
  }

  // --- Cookie parameters ---

  @Test
  fun `responds successfully when required cookie is present and matches schema type`() {
    // Given
    val operation = apiOperation(
      path = "/v1/users",
      method = "GET",
      requestSchema = requestSchema(
        parameters = listOf(parameterSchema(Cookie("session"), integerDataType(), isRequired = true))
      ),
      responses = mapOf(
        200 to responseSchema(
          bodies = listOf(bodySchema(
            contentType = ContentType("application/json"),
            dataType = objectDataType(properties = mapOf("id" to integerDataType()))))
        )
      )
    )
    mockServer = MockServer(listOf(operation))
    mockServer.start()
    RestAssured.port = mockServer.port()

    // When / Then
    given()
      .accept("application/json")
      .cookie("session", "123")
      .get("/v1/users")
      .then()
      .assertThat()
      .statusCode(200)
      .body("id", notNullValue())
  }

  @Test
  fun `responds with 418 when required cookie is missing`() {
    // Given
    val operation = apiOperation(
      path = "/v1/users",
      method = "GET",
      requestSchema = requestSchema(
        parameters = listOf(parameterSchema(Cookie("session"), integerDataType(), isRequired = true))
      ),
      responses = mapOf(
        200 to responseSchema(
          bodies = listOf(bodySchema(
            contentType = ContentType("application/json"),
            dataType = objectDataType(properties = mapOf("id" to integerDataType()))))
        )
      )
    )
    mockServer = MockServer(listOf(operation))
    mockServer.start()
    RestAssured.port = mockServer.port()

    // When / Then
    given()
      .accept("application/json")
      .get("/v1/users")
      .then()
      .assertThat()
      .statusCode(418)
  }

  // --- Request body ---

  @Test
  fun `responds successfully when request body matches schema type`() {
    // Given
    val operation = apiOperation(
      path = "/v1/users",
      method = "POST",
      requestSchema = requestSchema(
        bodies = listOf(bodySchema(
          contentType = ContentType("application/json"),
          dataType = objectDataType(properties = mapOf("id" to integerDataType()))))
      ),
      responses = mapOf(
        201 to responseSchema(
          bodies = listOf(bodySchema(
            contentType = ContentType("application/json"),
            dataType = objectDataType(properties = mapOf("id" to integerDataType()))))
        )
      )
    )
    mockServer = MockServer(listOf(operation))
    mockServer.start()
    RestAssured.port = mockServer.port()

    // When / Then
    given()
      .accept("application/json")
      .contentType("application/json")
      .body("""{"id": 42}""")
      .post("/v1/users")
      .then()
      .assertThat()
      .statusCode(201)
      .body("id", notNullValue())
  }

  @Test
  fun `responds with 418 when request body does not match schema type`() {
    // Given
    val operation = apiOperation(
      path = "/v1/users",
      method = "POST",
      requestSchema = requestSchema(
        bodies = listOf(bodySchema(
          contentType = ContentType("application/json"),
          dataType = objectDataType(properties = mapOf("id" to integerDataType()))))
      ),
      responses = mapOf(
        201 to responseSchema(
          bodies = listOf(bodySchema(
            contentType = ContentType("application/json"),
            dataType = objectDataType(properties = mapOf("id" to integerDataType()))))
        )
      )
    )
    mockServer = MockServer(listOf(operation))
    mockServer.start()
    RestAssured.port = mockServer.port()

    // When / Then
    given()
      .accept("application/json")
      .contentType("application/json")
      .body("""{"id": "john"}""")
      .post("/v1/users")
      .then()
      .assertThat()
      .statusCode(418)
  }

  @Test
  fun `responds with 418 when request content type does not match body schema`() {
    // Given
    val operation = apiOperation(
      path = "/v1/users",
      method = "POST",
      requestSchema = requestSchema(
        bodies = listOf(bodySchema(
          contentType = ContentType("application/json"),
          dataType = objectDataType(properties = mapOf("id" to integerDataType()))))
      ),
      responses = mapOf(
        201 to responseSchema(
          bodies = listOf(bodySchema(
            contentType = ContentType("application/json"),
            dataType = objectDataType(properties = mapOf("id" to integerDataType()))))
        )
      )
    )
    mockServer = MockServer(listOf(operation))
    mockServer.start()
    RestAssured.port = mockServer.port()

    // When / Then
    given()
      .accept("application/json")
      .contentType("text/plain")
      .body("""{"id": 42}""")
      .post("/v1/users")
      .then()
      .assertThat()
      .statusCode(418)
  }

  @Test
  fun `responds successfully when request body matches multiple oneOf variants`() {
    // Given
    val variantA = objectDataType(properties = mapOf("name" to stringDataType()))
    val variantB = objectDataType(properties = mapOf("name" to stringDataType(), "age" to integerDataType()))
    val operation = apiOperation(
      path = "/v1/users",
      method = "POST",
      requestSchema = requestSchema(
        bodies = listOf(bodySchema(
          contentType = ContentType("application/json"),
          dataType = oneOfDataType(subTypes = listOf(variantA, variantB))))
      ),
      responses = mapOf(
        201 to responseSchema(
          bodies = listOf(bodySchema(
            contentType = ContentType("application/json"),
            dataType = objectDataType(properties = mapOf("id" to integerDataType()))))
        )
      )
    )
    mockServer = MockServer(listOf(operation))
    mockServer.start()
    RestAssured.port = mockServer.port()

    // When / Then
    given()
      .accept("application/json")
      .contentType("application/json")
      .body("""{"name": "john"}""")
      .post("/v1/users")
      .then()
      .assertThat()
      .statusCode(201)
  }

  @Test
  fun `responds with 418 when request body matches no oneOf variant`() {
    // Given
    val variantA = objectDataType(properties = mapOf("name" to stringDataType()), requiredProperties = setOf("name"))
    val variantB = objectDataType(properties = mapOf("age" to integerDataType()), requiredProperties = setOf("age"))
    val operation = apiOperation(
      path = "/v1/users",
      method = "POST",
      requestSchema = requestSchema(
        bodies = listOf(bodySchema(
          contentType = ContentType("application/json"),
          dataType = oneOfDataType(subTypes = listOf(variantA, variantB))))
      ),
      responses = mapOf(
        201 to responseSchema(
          bodies = listOf(bodySchema(
            contentType = ContentType("application/json"),
            dataType = objectDataType(properties = mapOf("id" to integerDataType()))))
        )
      )
    )
    mockServer = MockServer(listOf(operation))
    mockServer.start()
    RestAssured.port = mockServer.port()

    // When / Then
    given()
      .accept("application/json")
      .contentType("application/json")
      .body("""{"unknown": true}""")
      .post("/v1/users")
      .then()
      .assertThat()
      .statusCode(418)
  }

  @Test
  fun `responds with 418 when required request body is missing`() {
    // Given
    val operation = apiOperation(
      path = "/v1/users",
      method = "POST",
      requestSchema = requestSchema(
        bodies = listOf(
          bodySchema(
            contentType = ContentType("application/json"),
            dataType = objectDataType(properties = mapOf("id" to integerDataType())),
            isRequired = true
          )
        )
      ),
      responses = mapOf(
        201 to responseSchema(
          bodies = listOf(bodySchema(
            contentType = ContentType("application/json"),
            dataType = objectDataType(properties = mapOf("id" to integerDataType()))))
        )
      )
    )
    mockServer = MockServer(listOf(operation))
    mockServer.start()
    RestAssured.port = mockServer.port()

    // When / Then
    given()
      .post("/v1/users")
      .then()
      .assertThat()
      .statusCode(418)
  }
}
