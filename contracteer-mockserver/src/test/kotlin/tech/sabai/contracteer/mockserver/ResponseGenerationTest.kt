package tech.sabai.contracteer.mockserver

import io.restassured.RestAssured
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.Matchers.emptyOrNullString
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.AfterEach
import tech.sabai.contracteer.core.operation.ContentType
import tech.sabai.contracteer.core.operation.ParameterElement.Header
import tech.sabai.contracteer.core.operation.ParameterElement.PathParam
import tech.sabai.contracteer.core.operation.ScenarioBody
import tech.sabai.contracteer.mockserver.TestFixture.apiOperation
import tech.sabai.contracteer.mockserver.TestFixture.bodySchema
import tech.sabai.contracteer.core.TestFixture.integerDataType
import tech.sabai.contracteer.core.TestFixture.objectDataType
import tech.sabai.contracteer.core.TestFixture.stringDataType
import tech.sabai.contracteer.mockserver.TestFixture.parameterSchema
import tech.sabai.contracteer.mockserver.TestFixture.requestSchema
import tech.sabai.contracteer.mockserver.TestFixture.responseSchema
import tech.sabai.contracteer.mockserver.TestFixture.scenario
import kotlin.test.Test

class ResponseGenerationTest {

  private lateinit var mockServer: MockServer

  @AfterEach
  fun tearDown() {
    mockServer.stop()
  }

  @Test
  fun `returns response body with scenario values when scenario matches`() {
    // Given
    val operation = apiOperation(
      path = "/v1/users/{id}",
      method = "GET",
      requestSchema = requestSchema(
        parameters = listOf(parameterSchema(PathParam("id"), integerDataType()))
      ),
      responses = mapOf(
        200 to responseSchema(
          headers = listOf(parameterSchema(Header("X-Request-Id"), stringDataType())),
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
          responseHeaders = mapOf(Header("X-Request-Id") to "abc-123"),
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
      .header("X-Request-Id", equalTo("abc-123"))
      .body("id", equalTo(42))
      .body("name", equalTo("John"))
  }

  @Test
  fun `returns response body with generated values when no scenario matches`() {
    // Given
    val operation = apiOperation(
      path = "/v1/users/{id}",
      method = "GET",
      requestSchema = requestSchema(
        parameters = listOf(parameterSchema(PathParam("id"), integerDataType()))
      ),
      responses = mapOf(
        200 to responseSchema(
          headers = listOf(parameterSchema(Header("X-Correlation-Id"), stringDataType())),
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
      .header("X-Correlation-Id", notNullValue())
      .body("id", notNullValue())
  }

  @Test
  fun `returns response with no body when response schema has no body`() {
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
          headers = listOf(parameterSchema(Header("X-Created-Id"), stringDataType()))
        )
      )
    )
    mockServer = MockServer(listOf(operation))
    mockServer.start()
    RestAssured.port = mockServer.port()

    // When / Then
    given()
      .contentType("application/json")
      .body("""{"id": 42}""")
      .post("/v1/users")
      .then()
      .assertThat()
      .statusCode(201)
      .header("X-Created-Id", notNullValue())
      .contentType(`is`(emptyOrNullString()))
      .body(`is`(emptyOrNullString()))
  }

  @Test
  fun `returns response with correct status code from scenario`() {
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
      .body("""{"id": 42}""")
      .post("/v1/users")
      .then()
      .assertThat()
      .statusCode(201)
      .body("id", equalTo(42))
  }

  @Test
  fun `returns response with correct content type from response schema`() {
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
            dataType = objectDataType(properties = mapOf("id" to integerDataType()))
          ))
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
      .contentType("application/json")
  }
}
