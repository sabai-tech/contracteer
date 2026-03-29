package tech.sabai.contracteer.mockserver

import io.restassured.RestAssured
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.jupiter.api.AfterEach
import tech.sabai.contracteer.core.operation.ContentType
import tech.sabai.contracteer.core.operation.ParameterElement.PathParam
import tech.sabai.contracteer.mockserver.TestFixture.apiOperation
import tech.sabai.contracteer.mockserver.TestFixture.bodySchema
import tech.sabai.contracteer.mockserver.TestFixture.integerDataType
import tech.sabai.contracteer.mockserver.TestFixture.objectDataType
import tech.sabai.contracteer.mockserver.TestFixture.parameterSchema
import tech.sabai.contracteer.mockserver.TestFixture.requestSchema
import tech.sabai.contracteer.mockserver.TestFixture.responseSchema
import kotlin.test.Test

class ContentNegotiationTest {

  private lateinit var mockServer: MockServer

  @AfterEach
  fun tearDown() {
    mockServer.stop()
  }

  @Test
  fun `responds successfully when Accept header matches response content type`() {
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
  fun `responds with 418 when Accept header does not match response content type`() {
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
      .accept("text/plain")
      .get("/v1/users/123")
      .then()
      .assertThat()
      .statusCode(418)
  }

  @Test
  fun `responds successfully when Accept header is not specified and single response content type exists`() {
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
      .get("/v1/users/123")
      .then()
      .assertThat()
      .statusCode(200)
      .body("id", notNullValue())
  }

  @Test
  fun `responds with 418 when Accept header is not specified and multiple response content types exist`() {
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
            bodySchema(
              contentType = ContentType("application/json"),
              dataType = objectDataType(properties = mapOf("id" to integerDataType()))
            ),
            bodySchema(
              contentType = ContentType("application/xml"),
              dataType = objectDataType(properties = mapOf("id" to integerDataType()))
            )
          )
        )
      )
    )
    mockServer = MockServer(listOf(operation))
    mockServer.start()
    RestAssured.port = mockServer.port()

    // When / Then
    given()
      .get("/v1/users/123")
      .then()
      .assertThat()
      .statusCode(418)
  }

  @Test
  fun `responds successfully when Accept header contains multiple types including a match`() {
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
      .accept("text/plain, application/json")
      .get("/v1/users/123")
      .then()
      .assertThat()
      .statusCode(200)
      .body("id", notNullValue())
  }

  @Test
  fun `responds successfully when Accept header uses subtype wildcard`() {
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
      .accept("application/*")
      .get("/v1/users/123")
      .then()
      .assertThat()
      .statusCode(200)
      .body("id", notNullValue())
  }

  @Test
  fun `disambiguates multiple response types using Accept quality factors`() {
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
            bodySchema(
              contentType = ContentType("application/json"),
              dataType = objectDataType(properties = mapOf("id" to integerDataType()))
            ),
            bodySchema(
              contentType = ContentType("application/xml"),
              dataType = objectDataType(properties = mapOf("id" to integerDataType()))
            )
          )
        )
      )
    )
    mockServer = MockServer(listOf(operation))
    mockServer.start()
    RestAssured.port = mockServer.port()

    // When / Then
    given()
      .accept("application/xml;q=0.5, application/json;q=0.9")
      .get("/v1/users/123")
      .then()
      .assertThat()
      .statusCode(200)
      .contentType("application/json")
  }

  @Test
  fun `responds successfully when Accept is wildcard`() {
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
      .accept("*/*")
      .get("/v1/users/123")
      .then()
      .assertThat()
      .statusCode(200)
      .body("id", notNullValue())
  }
}
