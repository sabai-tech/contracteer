package tech.sabai.contracteer.mockserver

import io.restassured.RestAssured
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.jupiter.api.AfterEach
import tech.sabai.contracteer.core.operation.ContentType
import tech.sabai.contracteer.core.operation.ParameterElement.PathParam
import tech.sabai.contracteer.core.operation.ParameterElement.QueryParam
import tech.sabai.contracteer.core.operation.ScenarioBody
import tech.sabai.contracteer.mockserver.TestFixture.apiOperation
import tech.sabai.contracteer.mockserver.TestFixture.bodySchema
import tech.sabai.contracteer.mockserver.TestFixture.integerDataType
import tech.sabai.contracteer.mockserver.TestFixture.objectDataType
import tech.sabai.contracteer.mockserver.TestFixture.parameterSchema
import tech.sabai.contracteer.mockserver.TestFixture.requestSchema
import tech.sabai.contracteer.mockserver.TestFixture.responseSchema
import tech.sabai.contracteer.mockserver.TestFixture.scenario
import tech.sabai.contracteer.mockserver.TestFixture.stringDataType
import kotlin.test.Test

class ScenarioMatchingTest {

  private lateinit var mockServer: MockServer

  @AfterEach
  fun tearDown() {
    mockServer.stop()
  }

  @Test
  fun `responds with scenario status code and response when request matches scenario parameter value`() {
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
            dataType = objectDataType(properties = mapOf("id" to integerDataType(), "name" to stringDataType()))))
        )
      ),
      scenarios = listOf(
        scenario(
          path = "/v1/users/{id}",
          method = "GET",
          key = "specificUser",
          statusCode = 200,
          requestParameterValues = mapOf(PathParam("id") to 42),
          responseBody = ScenarioBody(
            contentType = ContentType("application/json"),
            value = mapOf("id" to 42, "name" to "John")
          )
        )
      )
    )
    mockServer = MockServer(listOf(operation))
    mockServer.start()
    RestAssured.port = mockServer.port()

    // When / Then
    given()
      .accept("application/json")
      .get("/v1/users/42")
      .then()
      .assertThat()
      .statusCode(200)
      .body("id", equalTo(42))
      .body("name", equalTo("John"))
  }

  @Test
  fun `responds with 2xx from schema when request does not match any scenario parameter value`() {
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
      ),
      scenarios = listOf(
        scenario(
          path = "/v1/users/{id}",
          method = "GET",
          key = "specificUser",
          statusCode = 200,
          requestParameterValues = mapOf(PathParam("id") to 42),
          responseBody = ScenarioBody(
            contentType = ContentType("application/json"),
            value = mapOf("id" to 42)
          )
        )
      )
    )
    mockServer = MockServer(listOf(operation))
    mockServer.start()
    RestAssured.port = mockServer.port()

    // When / Then
    given()
      .accept("application/json")
      .get("/v1/users/99")
      .then()
      .assertThat()
      .statusCode(200)
      .body("id", notNullValue())
  }

  @Test
  fun `responds with scenario response when request matches scenario body value`() {
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
            dataType = objectDataType(properties = mapOf("id" to integerDataType(), "name" to stringDataType()))))
        )
      ),
      scenarios = listOf(
        scenario(
          path = "/v1/users",
          method = "POST",
          key = "createUser",
          statusCode = 201,
          requestBody = ScenarioBody(
            contentType = ContentType("application/json"),
            value = mapOf("id" to 42)
          ),
          responseBody = ScenarioBody(
            contentType = ContentType("application/json"),
            value = mapOf("id" to 42, "name" to "Created")
          )
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
      .body("id", equalTo(42))
      .body("name", equalTo("Created"))
  }

  @Test
  fun `responds with 2xx from schema when request body does not match scenario body value`() {
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
      ),
      scenarios = listOf(
        scenario(
          path = "/v1/users",
          method = "POST",
          key = "createUser",
          statusCode = 201,
          requestBody = ScenarioBody(
            contentType = ContentType("application/json"),
            value = mapOf("id" to 42)
          ),
          responseBody = ScenarioBody(
            contentType = ContentType("application/json"),
            value = mapOf("id" to 42)
          )
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
      .body("""{"id": 99}""")
      .post("/v1/users")
      .then()
      .assertThat()
      .statusCode(201)
      .body("id", notNullValue())
  }

  @Test
  fun `ignores absent optional parameters during scenario matching`() {
    // Given
    val operation = apiOperation(
      path = "/v1/users",
      method = "GET",
      requestSchema = requestSchema(
        parameters = listOf(parameterSchema(QueryParam("status"), stringDataType(), isRequired = false))
      ),
      responses = mapOf(
        200 to responseSchema(
          bodies = listOf(bodySchema(
            contentType = ContentType("application/json"),
            dataType = objectDataType(properties = mapOf("count" to integerDataType()))))
        )
      ),
      scenarios = listOf(
        scenario(
          path = "/v1/users",
          method = "GET",
          key = "activeUsers",
          statusCode = 200,
          requestParameterValues = mapOf(QueryParam("status") to "active"),
          responseBody = ScenarioBody(
            contentType = ContentType("application/json"),
            value = mapOf("count" to 5)
          )
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
      .body("count", notNullValue())
  }

  @Test
  fun `matches scenario even when optional parameter not in scenario is present in request`() {
    // Given
    val operation = apiOperation(
      path = "/v1/users/{id}",
      method = "GET",
      requestSchema = requestSchema(
        parameters = listOf(
          parameterSchema(PathParam("id"), integerDataType()),
          parameterSchema(QueryParam("verbose"), stringDataType(), isRequired = false)
        )
      ),
      responses = mapOf(
        200 to responseSchema(
          bodies = listOf(bodySchema(
            contentType = ContentType("application/json"),
            dataType = objectDataType(properties = mapOf("id" to integerDataType(), "name" to stringDataType()))))
        )
      ),
      scenarios = listOf(
        scenario(
          path = "/v1/users/{id}",
          method = "GET",
          key = "specificUser",
          statusCode = 200,
          requestParameterValues = mapOf(PathParam("id") to 42),
          responseBody = ScenarioBody(
            contentType = ContentType("application/json"),
            value = mapOf("id" to 42, "name" to "John")
          )
        )
      )
    )
    mockServer = MockServer(listOf(operation))
    mockServer.start()
    RestAssured.port = mockServer.port()

    // When / Then
    given()
      .accept("application/json")
      .get("/v1/users/42?verbose=true")
      .then()
      .assertThat()
      .statusCode(200)
      .body("id", equalTo(42))
      .body("name", equalTo("John"))
  }

  @Test
  fun `scenario match has priority over schema only match`() {
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
            dataType = objectDataType(properties = mapOf("id" to integerDataType(), "name" to stringDataType()))))
        )
      ),
      scenarios = listOf(
        scenario(
          path = "/v1/users/{id}",
          method = "GET",
          key = "specificUser",
          statusCode = 200,
          requestParameterValues = mapOf(PathParam("id") to 42),
          responseBody = ScenarioBody(
            contentType = ContentType("application/json"),
            value = mapOf("id" to 42, "name" to "John")
          )
        )
      )
    )
    mockServer = MockServer(listOf(operation))
    mockServer.start()
    RestAssured.port = mockServer.port()

    // When / Then
    given()
      .accept("application/json")
      .get("/v1/users/42")
      .then()
      .assertThat()
      .statusCode(200)
      .body("name", equalTo("John"))
  }

  @Test
  fun `uses Accept header to disambiguate scenarios with different response content types`() {
    // Given
    val operation = apiOperation(
      path = "/v1/users/{id}",
      method = "GET",
      requestSchema = requestSchema(
        parameters = listOf(parameterSchema(PathParam("id"), integerDataType()))
      ),
      responses = mapOf(
        200 to responseSchema(
          bodies = listOf(
            bodySchema(contentType = ContentType("application/json"),
                       dataType = objectDataType(properties = mapOf("id" to integerDataType(), "name" to stringDataType()))),
            bodySchema(contentType = ContentType("text/plain"),
                       dataType = stringDataType())
          )
        )
      ),
      scenarios = listOf(
        scenario(
          path = "/v1/users/{id}", method = "GET", key = "user1", statusCode = 200,
          requestParameterValues = mapOf(PathParam("id") to 42),
          responseBody = ScenarioBody(ContentType("application/json"), mapOf("id" to 42, "name" to "John"))
        ),
        scenario(
          path = "/v1/users/{id}", method = "GET", key = "user1", statusCode = 200,
          requestParameterValues = mapOf(PathParam("id") to 42),
          responseBody = ScenarioBody(ContentType("text/plain"), "User 42: John")
        )
      )
    )
    mockServer = MockServer(listOf(operation))
    mockServer.start()
    RestAssured.port = mockServer.port()

    // When / Then
    given()
      .accept("application/json")
      .get("/v1/users/42")
      .then()
      .assertThat()
      .statusCode(200)
      .body("id", equalTo(42))
      .body("name", equalTo("John"))
  }

  @Test
  fun `responds with scenario for non 2xx status code`() {
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
        ),
        404 to responseSchema(
          bodies = listOf(bodySchema(
            contentType = ContentType("application/json"),
            dataType = objectDataType(properties = mapOf("error" to stringDataType()))))
        )
      ),
      scenarios = listOf(
        scenario(
          path = "/v1/users/{id}",
          method = "GET",
          key = "notFound",
          statusCode = 404,
          requestParameterValues = mapOf(PathParam("id") to 999),
          responseBody = ScenarioBody(
            contentType = ContentType("application/json"),
            value = mapOf("error" to "Not Found")
          )
        )
      )
    )
    mockServer = MockServer(listOf(operation))
    mockServer.start()
    RestAssured.port = mockServer.port()

    // When / Then
    given()
      .accept("application/json")
      .get("/v1/users/999")
      .then()
      .assertThat()
      .statusCode(404)
      .body("error", equalTo("Not Found"))
  }
}
