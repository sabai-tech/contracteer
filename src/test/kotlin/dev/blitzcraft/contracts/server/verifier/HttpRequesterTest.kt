package dev.blitzcraft.contracts.server.verifier

import dev.blitzcraft.contracts.core.*
import dev.blitzcraft.contracts.core.datatype.IntegerDataType
import dev.blitzcraft.contracts.core.datatype.ObjectDataType
import dev.blitzcraft.contracts.core.datatype.StringDataType
import org.mockserver.integration.ClientAndServer
import org.mockserver.integration.ClientAndServer.startClientAndServer
import org.mockserver.model.Cookie.cookie
import org.mockserver.model.Header.header
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.mockserver.model.JsonPathBody.jsonPath
import org.mockserver.model.Parameter.param
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test


class HttpRequesterTest {

  private lateinit var mockServer: ClientAndServer

  @BeforeTest
  fun startServer() {
    mockServer = startClientAndServer(8080)
    mockServer.`when`(request().withMethod("POST")).respond(response().withStatusCode(200))
  }

  @AfterTest
  fun stopServer() {
    mockServer.stop()
  }

  @Test
  fun `send a request`() {
    // given
    val contract = Contract(RequestContract(method = "POST",
                                            path = "/test/{id}",
                                            pathParameters = mapOf("id" to Property(IntegerDataType())),
                                            queryParameters = mapOf("q" to Property(IntegerDataType())),
                                            cookies = mapOf("val" to Property(IntegerDataType())),
                                            headers = mapOf("x-my-header" to Property(StringDataType())),
                                            body = Body("application/json",
                                                        ObjectDataType(mapOf("prop" to Property(IntegerDataType()))))
                            ),
                            ResponseContract(statusCode = 200,
                                             body = Body("application/json",
                                                         ObjectDataType(mapOf("property" to Property(IntegerDataType()))))
                            )
    )

    // when
    val response = HttpRequester(contract).sendRequest()

    // then
    assert(response.statusCode == 200)
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