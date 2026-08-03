package dev.contracteer.mockserver

import io.restassured.RestAssured
import io.restassured.RestAssured.given
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import dev.contracteer.core.dsl.*

class RouteSpecificityTest {

  private lateinit var mockServer: MockServer

  @AfterEach
  fun tearDown() {
    mockServer.stop()
  }

  @Test
  fun `routes to more specific path when a less specific path is also defined`() {
    // given
    val general = apiOperation("GET", "/resources/{id}") {
      request { pathParam("id", integerType()) }
      response(200)
    }
    val specific = apiOperation("GET", "/resources/{id}_download") {
      request { pathParam("id", integerType()) }
      response(200)
    }
    mockServer = MockServer(listOf(general, specific), 0)
    mockServer.start()
    RestAssured.port = mockServer.port()

    // when
    given()
      .get("/resources/42_download")
      // then
      .then()
      .statusCode(200)
  }

  @Test
  fun `treats paths with and without trailing slash as distinct routes`() {
    // given
    val jobsList = apiOperation("GET", "/jobs") {
      response(200) { jsonBody(arrayType(stringType())) }
    }
    val jobsDetail = apiOperation("GET", "/jobs/") {
      request {
        queryParam("jobId", stringType(), isRequired = true)
      }
      response(200) {
        jsonBody(objectType { properties { "id" to stringType() } })
      }
    }
    mockServer = MockServer(listOf(jobsList, jobsDetail), 0)
    mockServer.start()
    RestAssured.port = mockServer.port()

    // when
    val listBody = given().get("/jobs").then().statusCode(200).extract().asString()

    // then
    assert(listBody.trimStart().startsWith("["))

    // when
    val detailBody = given().queryParam("jobId", "abc").get("/jobs/").then().statusCode(200).extract().asString()

    // then
    assert(detailBody.trimStart().startsWith("{"))
  }

  @Test
  fun `treats parameterised paths with and without trailing slash as distinct routes`() {
    // given
    val withoutSlash = apiOperation("GET", "/items/{id}") {
      request { pathParam("id", integerType()) }
      response(200) { jsonBody(arrayType(stringType())) }
    }
    val withSlash = apiOperation("GET", "/items/{id}/") {
      request { pathParam("id", integerType()) }
      response(200) { jsonBody(objectType { properties { "id" to integerType() } }) }
    }
    mockServer = MockServer(listOf(withoutSlash, withSlash), 0)
    mockServer.start()
    RestAssured.port = mockServer.port()

    // when
    val noSlashBody = given().get("/items/42").then().statusCode(200).extract().asString()
    val withSlashBody = given().get("/items/42/").then().statusCode(200).extract().asString()

    // then
    assert(noSlashBody.trimStart().startsWith("["))
    assert(withSlashBody.trimStart().startsWith("{"))
  }
}
