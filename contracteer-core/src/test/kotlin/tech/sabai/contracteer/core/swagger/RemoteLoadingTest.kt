package tech.sabai.contracteer.core.swagger

import org.mockserver.integration.ClientAndServer
import org.mockserver.integration.ClientAndServer.startClientAndServer
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.mockserver.model.MediaType.APPLICATION_JSON
import tech.sabai.contracteer.core.assertFailure
import tech.sabai.contracteer.core.assertSuccess
import java.io.File
import kotlin.test.Test

class RemoteLoadingTest {

  @Test
  fun `fails when URL does not exist`() {
    // given
    val server = startServer()

    // when
    val result = OpenApiLoader.loadOperations("http://localhost:8080/not_found.yaml")

    // then
    result.assertFailure()
    server.stop()
  }

  @Test
  fun `succeeds from an existing remote url`() {
    // given
    val server = startServer()

    // when
    val operations = OpenApiLoader.loadOperations("http://localhost:${server.port}/oas3.yaml").assertSuccess()

    // then
    assert(operations.size == 1)
    server.stop()
  }

  private fun startServer(): ClientAndServer {
    val mockServer = startClientAndServer()
    mockServer
      .`when`(
        request()
          .withMethod("GET")
          .withPath("/oas3.yaml")
      )
      .respond(
        response()
          .withStatusCode(200)
          .withBody(
            File("src/test/resources/scenario/2xx_schema_with_4xx_scenario.yaml").readText(),
            APPLICATION_JSON
          )
      )
    return mockServer
  }
}
