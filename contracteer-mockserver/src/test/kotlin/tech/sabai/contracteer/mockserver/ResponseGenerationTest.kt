package tech.sabai.contracteer.mockserver

import io.restassured.RestAssured
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.Matchers.emptyOrNullString
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.AfterEach
import tech.sabai.contracteer.core.dsl.apiOperation
import tech.sabai.contracteer.core.dsl.integerType
import tech.sabai.contracteer.core.dsl.objectType
import tech.sabai.contracteer.core.dsl.stringType
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
    val operation = apiOperation("GET", "/v1/users/{id}") {
      request { pathParam("id", integerType()) }
      response(200) {
        header("X-Request-Id", stringType())
        jsonBody(objectType {
          properties {
            "id" to integerType()
            "name" to stringType()
          }
        })
      }
      scenario("specificUser", status = 200) {
        request { pathParam["id"] = 42 }
        response {
          header["X-Request-Id"] = "abc-123"
          jsonBody { "id" to 42; "name" to "John" }
        }
      }
    }
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
    val operation = apiOperation("GET", "/v1/users/{id}") {
      request { pathParam("id", integerType()) }
      response(200) {
        header("X-Correlation-Id", stringType())
        jsonBody(objectType { properties { "id" to integerType() } })
      }
    }
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
    val operation = apiOperation("POST", "/v1/users") {
      request {
        jsonBody(objectType { properties { "id" to integerType() } })
      }
      response(201) {
        header("X-Created-Id", stringType())
      }
    }
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
    val operation = apiOperation("POST", "/v1/users") {
      request {
        jsonBody(objectType { properties { "id" to integerType() } })
      }
      response(201) {
        jsonBody(objectType { properties { "id" to integerType() } })
      }
      scenario("createUser", status = 201) {
        request { jsonBody { "id" to 42 } }
        response { jsonBody { "id" to 42 } }
      }
    }
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
      .get("/v1/users/123")
      .then()
      .assertThat()
      .statusCode(200)
      .contentType("application/json")
  }
}
