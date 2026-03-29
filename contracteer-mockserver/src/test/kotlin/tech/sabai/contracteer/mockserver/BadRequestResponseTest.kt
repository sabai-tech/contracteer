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
import tech.sabai.contracteer.mockserver.TestFixture.stringDataType
import kotlin.test.Test

class BadRequestResponseTest {

  private lateinit var mockServer: MockServer

  @AfterEach
  fun tearDown() {
    mockServer.stop()
  }

  @Test
  fun `responds with 400 and generated body when validation fails and 400 response is defined`() {
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
        400 to responseSchema(
          bodies = listOf(bodySchema(
            contentType = ContentType("application/json"),
            dataType = objectDataType(properties = mapOf("error" to stringDataType()))))
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
      .statusCode(400)
      .contentType("application/json")
      .body("error", notNullValue())
  }

  @Test
  fun `responds with 418 when validation fails and no 400 response is defined`() {
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
}
