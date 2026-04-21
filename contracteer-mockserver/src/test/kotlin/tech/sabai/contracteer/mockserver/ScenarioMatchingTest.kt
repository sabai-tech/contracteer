package tech.sabai.contracteer.mockserver

import io.restassured.RestAssured
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.jupiter.api.AfterEach
import tech.sabai.contracteer.core.dsl.apiOperation
import tech.sabai.contracteer.core.dsl.integerType
import tech.sabai.contracteer.core.dsl.objectType
import tech.sabai.contracteer.core.dsl.stringType
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
    val operation = apiOperation("GET", "/v1/users/{id}") {
      request { pathParam("id", integerType()) }
      response(200) {
        jsonBody(objectType {
          properties {
            "id" to integerType()
            "name" to stringType()
          }
        })
      }
      scenario("specificUser", status = 200) {
        request { pathParam["id"] = 42 }
        response { jsonBody { "id" to 42; "name" to "John" } }
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
      .body("id", equalTo(42))
      .body("name", equalTo("John"))
  }

  @Test
  fun `responds with 2xx from schema when request does not match any scenario parameter value`() {
    // Given
    val operation = apiOperation("GET", "/v1/users/{id}") {
      request { pathParam("id", integerType()) }
      response(200) {
        jsonBody(objectType { properties { "id" to integerType() } })
      }
      scenario("specificUser", status = 200) {
        request { pathParam["id"] = 42 }
        response { jsonBody { "id" to 42 } }
      }
    }
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
    val operation = apiOperation("POST", "/v1/users") {
      request {
        jsonBody(objectType { properties { "id" to integerType() } })
      }
      response(201) {
        jsonBody(objectType {
          properties {
            "id" to integerType()
            "name" to stringType()
          }
        })
      }
      scenario("createUser", status = 201) {
        request { jsonBody { "id" to 42 } }
        response { jsonBody { "id" to 42; "name" to "Created" } }
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
      .body("name", equalTo("Created"))
  }

  @Test
  fun `responds with 2xx from schema when request body does not match scenario body value`() {
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
    val operation = apiOperation("GET", "/v1/users") {
      request { queryParam("status", stringType(), isRequired = false) }
      response(200) {
        jsonBody(objectType { properties { "count" to integerType() } })
      }
      scenario("activeUsers", status = 200) {
        request { queryParam["status"] = "active" }
        response { jsonBody { "count" to 5 } }
      }
    }
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
    val operation = apiOperation("GET", "/v1/users/{id}") {
      request {
        pathParam("id", integerType())
        queryParam("verbose", stringType(), isRequired = false)
      }
      response(200) {
        jsonBody(objectType {
          properties {
            "id" to integerType()
            "name" to stringType()
          }
        })
      }
      scenario("specificUser", status = 200) {
        request { pathParam["id"] = 42 }
        response { jsonBody { "id" to 42; "name" to "John" } }
      }
    }
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
    val operation = apiOperation("GET", "/v1/users/{id}") {
      request { pathParam("id", integerType()) }
      response(200) {
        jsonBody(objectType {
          properties {
            "id" to integerType()
            "name" to stringType()
          }
        })
      }
      scenario("specificUser", status = 200) {
        request { pathParam["id"] = 42 }
        response { jsonBody { "id" to 42; "name" to "John" } }
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
      .body("name", equalTo("John"))
  }

  @Test
  fun `uses Accept header to disambiguate scenarios with different response content types`() {
    // Given
    val operation = apiOperation("GET", "/v1/users/{id}") {
      request { pathParam("id", integerType()) }
      response(200) {
        jsonBody(objectType {
          properties {
            "id" to integerType()
            "name" to stringType()
          }
        })
        plainTextBody(stringType())
      }
      scenario("user1", status = 200) {
        request { pathParam["id"] = 42 }
        response { jsonBody { "id" to 42; "name" to "John" } }
      }
      scenario("user1", status = 200) {
        request { pathParam["id"] = 42 }
        response { plainTextBody("User 42: John") }
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
      .body("id", equalTo(42))
      .body("name", equalTo("John"))
  }

  @Test
  fun `responds with scenario for non 2xx status code`() {
    // Given
    val operation = apiOperation("GET", "/v1/users/{id}") {
      request { pathParam("id", integerType()) }
      response(200) {
        jsonBody(objectType { properties { "id" to integerType() } })
      }
      response(404) {
        jsonBody(objectType { properties { "error" to stringType() } })
      }
      scenario("notFound", status = 404) {
        request { pathParam["id"] = 999 }
        response { jsonBody { "error" to "Not Found" } }
      }
    }
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
