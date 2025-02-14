package tech.sabai.contracteer.verifier

import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import tech.sabai.contracteer.core.contract.*
import tech.sabai.contracteer.core.datatype.IntegerDataType
import tech.sabai.contracteer.core.datatype.ObjectDataType
import tech.sabai.contracteer.core.datatype.StringDataType
import kotlin.test.Test

class ServerVerifierTest {

  private val contract = Contract(
    request = ContractRequest(
      method = "GET",
      path = "/product/{id}",
      pathParameters = listOf(PathParameter("id", IntegerDataType()))),
    response = ContractResponse(
      statusCode = 200,
      body = Body(contentType = "application/json",
                  dataType = ObjectDataType(properties = mapOf(
                    "id" to IntegerDataType(),
                    "name" to StringDataType()))))
  )

  @Test
  fun `Validates successfully a Contract `() {
    // given
    val app = routes(
      "/product/{id}" bind GET to {
        Response(OK).header("Content-type", "application/json").body("""{ "id": 123, "name": "John Doe"}""")
      }
    )
    val server = app.asServer(SunHttp(0)).start()

    // when
    val validationResult = ServerVerifier(ServerConfiguration(port = server.port())).verify(contract)

    // then
    assert(validationResult.isSuccess())
  }

  @Test
  fun `Does not validate a Contract`() {
    // given
    val app = routes(
      "/product/{id}" bind GET to {
        Response(OK).header("Content-type", "application/json").body("""{ "id": "bad Id", "name": "John Doe"}""")
      }
    )
    val server = app.asServer(SunHttp(0)).start()

    // when
    val validationResult = ServerVerifier(ServerConfiguration(port = server.port())).verify(contract)

    // then
    assert(validationResult.isFailure())
    assert(validationResult.errors().size == 1)
    assert(validationResult.errors().first().startsWith("'id'"))
  }
}