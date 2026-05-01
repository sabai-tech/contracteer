package tech.sabai.contracteer.mockserver

import io.restassured.RestAssured
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.junit.jupiter.api.AfterEach
import tech.sabai.contracteer.core.datatype.GenerationOutcome
import tech.sabai.contracteer.core.dsl.apiOperation
import tech.sabai.contracteer.core.dsl.cyclicObjectType
import tech.sabai.contracteer.core.dsl.integerType
import tech.sabai.contracteer.core.dsl.stringType
import kotlin.test.Test

class BoundaryResponseTest {

  private lateinit var mockServer: MockServer

  @AfterEach
  fun tearDown() {
    mockServer.stop()
  }

  @Test
  fun `responds with 418 and diagnostic when response body cannot be generated`() {
    // given
    val person = cyclicObjectType("Person") { proxy ->
      properties {
        "name" to stringType()
        "friend" to proxy
      }
      required("name", "friend")
    }
    val operation = apiOperation("GET", "/v1/persons/{id}") {
      request { pathParam("id", integerType()) }
      response(200) { jsonBody(person) }
    }
    mockServer = MockServer(listOf(operation))
    mockServer.start()
    RestAssured.port = mockServer.port()

    // when
    val response = given().accept("application/json").get("/v1/persons/1")

    // then
    response.then()
      .assertThat()
      .statusCode(418)
      .body(containsString("body.friend.friend"))
      .body(containsString(GenerationOutcome.Reason.CYCLE.explanation()))
  }

  @Test
  fun `responds with 200 and absorbs the cycle as null when the recursive property is nullable`() {
    // given — Person is nullable, so on cycle re-entry the outer object absorbs the inner
    // Boundary as a null friend slot instead of propagating it.
    val person = cyclicObjectType("Person", isNullable = true) { proxy ->
      properties {
        "name" to stringType()
        "friend" to proxy
      }
      required("name", "friend")
    }
    val operation = apiOperation("GET", "/v1/persons/{id}") {
      request { pathParam("id", integerType()) }
      response(200) { jsonBody(person) }
    }
    mockServer = MockServer(listOf(operation))
    mockServer.start()
    RestAssured.port = mockServer.port()

    // when
    val response = given().accept("application/json").get("/v1/persons/1")

    // then
    response.then()
      .assertThat()
      .statusCode(200)
      .body("name", notNullValue())
      .body("friend.friend", nullValue())
  }
}