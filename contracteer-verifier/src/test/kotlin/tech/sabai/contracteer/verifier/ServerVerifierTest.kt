package tech.sabai.contracteer.verifier

import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import tech.sabai.contracteer.core.operation.*
import tech.sabai.contracteer.core.serde.BasicSerde
import tech.sabai.contracteer.verifier.TestFixture.integerDataType
import tech.sabai.contracteer.verifier.TestFixture.objectDataType
import tech.sabai.contracteer.verifier.TestFixture.stringDataType
import kotlin.test.Test

class ServerVerifierTest {

  @Test
  fun `verifies scenario based case successfully`() {
    // Given
    val apiOperation = ApiOperation(
      path = "/users/{id}",
      method = "GET",
      requestSchema = RequestSchema(
        parameters = listOf(
          ParameterSchema(
            element = ParameterElement.PathParam("id"),
            dataType = integerDataType(),
            isRequired = true,
            serde = BasicSerde
          )
        ),
        bodies = emptyList()
      ),
      responses = mapOf(
        200 to ResponseSchema(
          headers = emptyList(),
          bodies = listOf(
            BodySchema(
              contentType = ContentType("application/json"),
              dataType = objectDataType(properties = mapOf("id" to integerDataType(), "name" to stringDataType())),
              isRequired = true
            )
          )
        ),
        404 to ResponseSchema(
          headers = emptyList(),
          bodies = emptyList()
        )
      ),
      scenarios = listOf(
        Scenario(
          path = "/users/{id}",
          method = "GET",
          key = "validUser",
          statusCode = 200,
          request = ScenarioRequest(
            parameterValues = mapOf(ParameterElement.PathParam("id") to 1),
            body = null
          ),
          response = ScenarioResponse(
            parameterValues = emptyMap(),
            body = ScenarioBody(
              contentType = ContentType("application/json"),
              value = mapOf("id" to 1, "name" to "John")
            )
          )
        ),
        Scenario(
          path = "/users/{id}",
          method = "GET",
          key = "notFound",
          statusCode = 404,
          request = ScenarioRequest(
            parameterValues = mapOf(ParameterElement.PathParam("id") to 999),
            body = null
          ),
          response = ScenarioResponse(
            parameterValues = emptyMap(),
            body = null
          )
        )
      )
    )

    val app = routes(
      "/users/1" bind GET to {
        Response(OK).header("Content-Type", "application/json").body("""{"id": 1, "name": "John"}""")
      },
      "/users/999" bind GET to {
        Response(NOT_FOUND)
      }
    )
    val server = app.asServer(SunHttp(0)).start()

    // When
    val cases = VerificationCaseFactory.create(apiOperation)
    val verifier = ServerVerifier(ServerConfiguration(port = server.port()))
    val results = cases.map { verifier.verify(it) }

    // Then
    server.stop()
    assert(results.size == 2)
    assert(results.all { it.result.isSuccess() })
  }

  @Test
  fun `generates missing request parameter values for scenario based case`() {
    // Given
    val apiOperation = ApiOperation(
      path = "/users/{userId}/orders/{orderId}",
      method = "GET",
      requestSchema = RequestSchema(
        parameters = listOf(
          ParameterSchema(
            element = ParameterElement.PathParam("userId"),
            dataType = integerDataType(),
            isRequired = true,
            serde = BasicSerde
          ),
          ParameterSchema(
            element = ParameterElement.PathParam("orderId"),
            dataType = integerDataType(),
            isRequired = true,
            serde = BasicSerde
          )
        ),
        bodies = emptyList()
      ),
      responses = mapOf(
        200 to ResponseSchema(
          headers = emptyList(),
          bodies = listOf(
            BodySchema(
              contentType = ContentType("application/json"),
              dataType = objectDataType(properties = mapOf("id" to integerDataType(), "name" to stringDataType())),
              isRequired = true
            )
          )
        )
      ),
      scenarios = listOf(
        Scenario(
          path = "/users/{userId}/orders/{orderId}",
          method = "GET",
          key = "validOrder",
          statusCode = 200,
          request = ScenarioRequest(
            parameterValues = mapOf(ParameterElement.PathParam("userId") to 1),
            body = null
          ),
          response = ScenarioResponse(
            parameterValues = emptyMap(),
            body = ScenarioBody(
              contentType = ContentType("application/json"),
              value = mapOf("id" to 1, "name" to "Order")
            )
          )
        )
      )
    )

    val app = routes(
      "/users/{userId}/orders/{orderId}" bind GET to { request ->
        val orderId = request.uri.path.split("/").last()
        if (orderId.matches(Regex("-?\\d+")))
          Response(OK).header("Content-Type", "application/json").body("""{"id": 1, "name": "Order"}""")
        else
          Response(NOT_FOUND)
      }
    )
    val server = app.asServer(SunHttp(0)).start()

    // When
    val cases = VerificationCaseFactory.create(apiOperation)
    val verifier = ServerVerifier(ServerConfiguration(port = server.port()))
    val results = cases.map { verifier.verify(it) }

    // Then
    server.stop()
    assert(results.size == 1)
    assert(results.all { it.result.isSuccess() })
  }

  @Test
  fun `returns failure when verification fails`() {
    // Given
    val apiOperation = ApiOperation(
      path = "/users/{id}",
      method = "GET",
      requestSchema = RequestSchema(
        parameters = listOf(
          ParameterSchema(
            element = ParameterElement.PathParam("id"),
            dataType = integerDataType(),
            isRequired = true,
            serde = BasicSerde
          )
        ),
        bodies = emptyList()
      ),
      responses = mapOf(
        200 to ResponseSchema(
          headers = emptyList(),
          bodies = listOf(
            BodySchema(
              contentType = ContentType("application/json"),
              dataType = objectDataType(properties = mapOf("id" to integerDataType(), "name" to stringDataType())),
              isRequired = true
            )
          )
        )
      ),
      scenarios = listOf(
        Scenario(
          path = "/users/{id}",
          method = "GET",
          key = "validUser",
          statusCode = 200,
          request = ScenarioRequest(
            parameterValues = mapOf(ParameterElement.PathParam("id") to 1),
            body = null
          ),
          response = ScenarioResponse(
            parameterValues = emptyMap(),
            body = ScenarioBody(
              contentType = ContentType("application/json"),
              value = mapOf("id" to 1, "name" to "John")
            )
          )
        )
      )
    )

    val app = routes(
      "/users/{id}" bind GET to {
        Response(OK).header("Content-Type", "application/json").body("""{"id": "invalid", "name": "John"}""")
      }
    )
    val server = app.asServer(SunHttp(0)).start()

    // When
    val cases = VerificationCaseFactory.create(apiOperation)
    val verifier = ServerVerifier(ServerConfiguration(port = server.port()))
    val results = cases.map { verifier.verify(it) }

    // Then
    server.stop()
    assert(results.size == 1)
    assert(results[0].result.isFailure())
    assert(results[0].result.errors().isNotEmpty())
  }
}
