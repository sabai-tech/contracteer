package tech.sabai.contracteer.verifier

import org.http4k.core.Status
import org.mockserver.integration.ClientAndServer
import org.mockserver.integration.ClientAndServer.startClientAndServer
import org.mockserver.model.Cookie.cookie
import org.mockserver.model.Header.header
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.mockserver.model.JsonPathBody.jsonPath
import org.mockserver.model.Parameter.param
import tech.sabai.contracteer.core.contract.*
import tech.sabai.contracteer.verifier.TestFixture.body
import tech.sabai.contracteer.verifier.TestFixture.integerDataType
import tech.sabai.contracteer.verifier.TestFixture.objectDataType
import tech.sabai.contracteer.verifier.TestFixture.parameter
import tech.sabai.contracteer.verifier.TestFixture.pathParameter
import tech.sabai.contracteer.verifier.TestFixture.stringDataType
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test


class HttpRequesterTest {

  private lateinit var mockServer: ClientAndServer

  @BeforeTest
  fun startServer() {
    mockServer = startClientAndServer()
    mockServer.`when`(request().withMethod("POST")).respond(response().withStatusCode(200))
  }

  @AfterTest
  fun stopServer() {
    mockServer.stop()
  }

  @Test
  fun `send a request`() {
    // given
    val contract = Contract(
      ContractRequest(
        method = "POST",
        path = "/test/{id}",
        pathParameters = listOf(pathParameter("id", integerDataType())),
        queryParameters = listOf(parameter("q", integerDataType())),
        cookies = listOf(parameter("val", integerDataType())),
        headers = listOf(parameter("x-my-header", stringDataType())),
        body = body(contentType = ContentType("application/json"), objectDataType(properties = mapOf("prop" to integerDataType())))
      ),
      ContractResponse(
        statusCode = 200,
        body = body(contentType = ContentType("application/json"), objectDataType(properties = mapOf("property" to integerDataType())))
      )
    )

    // when
    val response = HttpRequester("http://localhost:${mockServer.port}").sendRequestFor(contract)

    // then
    assert(response.status == Status.OK)
    mockServer.verify(
      request()
        .withMethod("POST")
        .withPath("/test/{id}")
        .withPathParameter(param("id", "-?(\\d+)"))
        .withQueryStringParameters(param("q", "-?(\\d+)"))
        .withCookie(cookie("val", "-?(\\d+)"))
        .withHeader(header("x-my-header", ".*"))
        .withHeader(header("Content-Type", "application/json"))
        .withHeader(header("Accept", "application/json"))
        .withBody(jsonPath("$[?(@['prop'] =~ /-?(\\d+)/)]"))
    )
  }
}