package tech.sabai.contracteer.mockserver

import io.restassured.RestAssured
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.jupiter.api.AfterEach
import tech.sabai.contracteer.core.dsl.apiOperation
import tech.sabai.contracteer.core.dsl.integerType
import tech.sabai.contracteer.core.dsl.objectType
import tech.sabai.contracteer.core.dsl.stringType
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
    val operation = apiOperation("GET", "/v1/users/{id}") {
      request { pathParam("id", integerType()) }
      response(200) {
        jsonBody(objectType { properties { "id" to integerType() } })
      }
      response(400) {
        jsonBody(objectType { properties { "error" to stringType() } })
      }
    }
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
    val operation = apiOperation("GET", "/v1/users/{id}") {
      request { pathParam("id", integerType()) }
      response(200) {
        jsonBody(objectType { properties { "id" to integerType() } })
      }
    }
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
