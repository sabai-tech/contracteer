package tech.sabai.contracteer.mockserver

import io.restassured.RestAssured
import io.restassured.RestAssured.given
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.operation.ParameterElement.PathParam
import tech.sabai.contracteer.mockserver.TestFixture.apiOperation
import tech.sabai.contracteer.mockserver.TestFixture.integerDataType
import tech.sabai.contracteer.mockserver.TestFixture.parameterSchema
import tech.sabai.contracteer.mockserver.TestFixture.requestSchema
import tech.sabai.contracteer.mockserver.TestFixture.responseSchema

class RouteSpecificityTest {

  private lateinit var mockServer: MockServer

  @AfterEach
  fun tearDown() {
    mockServer.stop()
  }

  @Test
  fun `routes to more specific path when a less specific path is also defined`() {
    // given
    val general = apiOperation(
      path = "/resources/{id}",
      method = "GET",
      requestSchema = requestSchema(
        parameters = listOf(parameterSchema(PathParam("id"), integerDataType()))
      ),
      responses = mapOf(200 to responseSchema())
    )
    val specific = apiOperation(
      path = "/resources/{id}_download",
      method = "GET",
      requestSchema = requestSchema(
        parameters = listOf(parameterSchema(PathParam("id"), integerDataType()))
      ),
      responses = mapOf(200 to responseSchema())
    )
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
