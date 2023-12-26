package dev.blitzcraft.contracts.verifier

import dev.blitzcraft.contracts.core.*
import dev.blitzcraft.contracts.core.datatype.IntegerDataType
import dev.blitzcraft.contracts.core.datatype.ObjectDataType
import dev.blitzcraft.contracts.core.datatype.StringDataType
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Jetty
import org.http4k.server.asServer
import kotlin.test.Test

class ServerVerifierTest {

  private val contract = Contract(
    request = RequestContract(
      method = "GET",
      path = "/product/{id}",
      pathParameters = listOf(Property("id", IntegerDataType()))),
    response = ResponseContract(
      statusCode = 200,
      body = Body(contentType = "application/json",
                  dataType = ObjectDataType(listOf(
                    Property("id", IntegerDataType()),
                    Property("name", StringDataType())))))
  )

  @Test
  fun `validate a Contract successfully`() {
    // given
    val app = routes(
      "/product/{id}" bind GET to {
        Response(OK).header("Content-type", "application/json").body("""{ "id": 123, "name": "John Doe"}""")
      }
    )
    val server = app.asServer(Jetty(0)).start()

    // when
    val validationResult = ServerVerifier(serverPort = server.port()).verify(contract)

    // then
    assert(validationResult.isSuccess())
  }

  @Test
  fun `fail to validate a Contract`() {
    // given
    val app = routes(
      "/product/{id}" bind GET to {
        Response(OK).header("Content-type", "application/json").body("""{ "id": "bad Id", "name": "John Doe"}""")
      }
    )
    val server = app.asServer(Jetty(0)).start()

    // when
    val validationResult = ServerVerifier(serverPort = server.port()).verify(contract)

    // expect fails
    assert(validationResult.isSuccess().not())
    assert(validationResult.errors().size == 1)
    assert(validationResult.errors().first().startsWith("id"))
  }
}