package tech.sabai.contracteer.mockserver

import io.restassured.RestAssured
import io.restassured.RestAssured.given
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.dsl.apiOperation
import tech.sabai.contracteer.core.dsl.integerType

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
}
