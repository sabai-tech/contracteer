package tech.sabai.contracteer.verifier

import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import tech.sabai.contracteer.core.dsl.apiOperation
import tech.sabai.contracteer.core.dsl.integerType
import tech.sabai.contracteer.core.dsl.objectType
import tech.sabai.contracteer.core.dsl.stringType
import tech.sabai.contracteer.core.operation.ParameterElement.PathParam
import tech.sabai.contracteer.verifier.VerificationCase.TypeMismatch
import kotlin.test.Test

class TypeMismatchVerificationTest {

  @Test
  fun `verifies body type mismatch sends mutated value as raw body`() {
    // Given
    var capturedBody: String? = null
    var capturedContentType: String? = null

    val app = routes(
      "/users" bind POST to { request ->
        capturedBody = request.bodyString()
        capturedContentType = request.header("Content-Type")
        Response(BAD_REQUEST)
          .header("Content-Type", "application/json")
          .body("""{"error": "invalid body"}""")
      }
    )
    val server = app.asServer(SunHttp(0)).start()

    val apiOperation = apiOperation("POST", "/users") {
      request {
        jsonBody(objectType {
          properties { "name" to stringType() }
        })
      }

      response(200) {}

      response(400) {
        jsonBody(objectType {
          properties { "error" to stringType() }
        })
      }
    }

    val cases = VerificationCaseFactory.create(apiOperation)
    val typeMismatchCase = cases.filterIsInstance<TypeMismatch>().first()
    val verifier = ServerVerifier(ServerConfiguration(port = server.port()))

    // When
    val outcome = verifier.verify(typeMismatchCase)

    // Then
    server.stop()
    assert(outcome.result.isSuccess())
    assert(capturedBody == "<<not a object>>")
    assert(capturedContentType?.contains("application/json") == true)
  }

  @Test
  fun `verifies parameter type mismatch sends mutated value for targeted parameter`() {
    // Given
    var capturedId: String? = null

    val app = routes(
      "/users/{id}" bind GET to { request ->
        capturedId = request.path("id")
        Response(BAD_REQUEST)
          .header("Content-Type", "application/json")
          .body("""{"error": "invalid id"}""")
      }
    )
    val server = app.asServer(SunHttp(0)).start()

    val apiOperation = apiOperation("GET", "/users/{id}") {
      request {
        pathParam("id", integerType())
      }

      response(200) {}

      response(400) {
        jsonBody(objectType {
          properties { "error" to stringType() }
        })
      }
    }

    val cases = VerificationCaseFactory.create(apiOperation)
    val typeMismatchCase = cases.filterIsInstance<TypeMismatch>().first()
    val verifier = ServerVerifier(ServerConfiguration(port = server.port()))

    // When
    val outcome = verifier.verify(typeMismatchCase)

    // Then
    server.stop()
    assert(outcome.result.isSuccess())
    assert(capturedId == "<<not a integer>>")
  }

  @Test
  fun `sends valid values for non-mutated parameters alongside mutated one`() {
    // Given
    var capturedId: String? = null
    var capturedPage: String? = null

    val app = routes(
      "/users/{id}" bind GET to { request ->
        capturedId = request.path("id")
        capturedPage = request.query("page")
        Response(BAD_REQUEST)
          .header("Content-Type", "application/json")
          .body("""{"error": "invalid"}""")
      }
    )
    val server = app.asServer(SunHttp(0)).start()

    val apiOperation = apiOperation("GET", "/users/{id}") {
      request {
        pathParam("id", integerType())
        queryParam("page", integerType())
      }

      response(200) {}

      response(400) {
        jsonBody(objectType {
          properties { "error" to stringType() }
        })
      }
    }

    val cases = VerificationCaseFactory.create(apiOperation)
    // The factory generates 2 cases: one for path, one for query. Get the path one.
    val pathCase = cases.filterIsInstance<TypeMismatch>()
      .first { it.mutatedElement == MutatedElement.Parameter(PathParam("id")) }
    val verifier = ServerVerifier(ServerConfiguration(port = server.port()))

    // When
    val outcome = verifier.verify(pathCase)

    // Then
    server.stop()
    assert(outcome.result.isSuccess())
    assert(capturedId == "<<not a integer>>")
    // The non-mutated query param should have a valid integer value
    assert(capturedPage != null)
    assert(capturedPage!!.matches(Regex("-?\\d+")))
  }

  @Test
  fun `verification fails when server does not return 400`() {
    // Given
    val app = routes(
      "/users" bind POST to {
        Response(OK)
          .header("Content-Type", "application/json")
          .body("""{"id": 1}""")
      }
    )
    val server = app.asServer(SunHttp(0)).start()

    val apiOperation = apiOperation("POST", "/users") {
      request {
        jsonBody(objectType {
          properties { "name" to stringType() }
        })
      }

      response(200) {}

      response(400) {
        jsonBody(objectType {
          properties { "error" to stringType() }
        })
      }
    }

    val cases = VerificationCaseFactory.create(apiOperation)
    val typeMismatchCase = cases.filterIsInstance<TypeMismatch>().first()
    val verifier = ServerVerifier(ServerConfiguration(port = server.port()))

    // When
    val outcome = verifier.verify(typeMismatchCase)

    // Then
    server.stop()
    assert(outcome.result.isFailure())
    assert(outcome.result.errors().any { it.contains("Status code") })
  }
}
