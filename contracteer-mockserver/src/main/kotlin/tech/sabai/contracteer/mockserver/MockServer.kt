package tech.sabai.contracteer.mockserver

import io.github.oshai.kotlinlogging.KotlinLogging
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.I_M_A_TEAPOT
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.operation.ApiOperation
import tech.sabai.contracteer.core.operation.BodySchema
import tech.sabai.contracteer.core.operation.ResponseSchema
import tech.sabai.contracteer.core.operation.Scenario

/**
 * An HTTP mock server that serves responses derived from OpenAPI [ApiOperation] definitions.
 *
 * The mock server validates incoming requests against the operation's request schema,
 * matches scenarios for deterministic responses, and falls back to schema-generated
 * random values when no scenario matches.
 *
 * @param operations the API operations to serve
 * @param port the port to listen on, or 0 for a random available port
 */
class MockServer @JvmOverloads constructor(private val operations: List<ApiOperation>,
                                           private val port: Int = 0) {

  private lateinit var http4kServer: Http4kServer
  private val logger = KotlinLogging.logger {}
  private val httpLogger = KotlinLogging.logger("tech.sabai.contracteer.http")

  /** Starts the mock server. */
  fun start() {
    val routeHandlers = operations
      .onEach { logger.info { "Registering route: [${it.method.uppercase()}] ${it.path}" } }
      .map { createRouteHandler(it) }

    logger.info { "Starting Contracteer mock server" }
    http4kServer = httpHandlerFrom(routeHandlers).asServer(SunHttp(port)).start()
    logger.info { "Contracteer mock server started on port ${this.port()}" }
  }

  /** Stops the mock server. */
  fun stop() {
    if (::http4kServer.isInitialized) {
      logger.info { "Stopping Contracteer mock server" }
      http4kServer.stop()
      logger.info { "Stopped Contracteer mock server" }
    }
  }

  /** Returns the port the server is listening on. Must be called after [start]. */
  fun port(): Int {
    check(::http4kServer.isInitialized) { "Contracteer mock server is not started yet." }
    return http4kServer.port()
  }

  private fun httpHandlerFrom(routeHandlers: List<RoutingHttpHandler>) =
    routes(*routeHandlers.toTypedArray())

  private fun createRouteHandler(operation: ApiOperation) =
    operation.path bind Method.valueOf(operation.method.uppercase()) to { request -> handleRequest(request, operation) }

  private fun handleRequest(request: Request, operation: ApiOperation): Response {
    httpLogger.debug { formatRequest(request) }

    val response = processRequest(request, operation)

    httpLogger.debug { formatResponse(response) }

    if (response.status == I_M_A_TEAPOT) {
      httpLogger.warn {
        "Request handling failed for [${operation.method.uppercase()}] ${operation.path}\n${
          formatRequest(request)
        }\n${formatResponse(response)}"
      }
      httpLogger.warn { "Enable DEBUG logging for 'tech.sabai.contracteer.http' to see all HTTP traffic" }
    }

    return response
  }

  private fun processRequest(request: Request, operation: ApiOperation): Response {
    val validationResult = operation.requestSchema.validate(request)
    if (validationResult.isFailure()) {
      val badRequestResponseSchema = operation.badRequestResponse()
      return if (badRequestResponseSchema != null)
        ResponseGenerator.fromSchema(400,
                                     badRequestResponseSchema.headers,
                                     badRequestResponseSchema.bodies.firstOrNull())
      else
        validationErrorResponse(operation, validationResult.errors())
    }

    return when (val matchResult = ScenarioMatcher.match(request, operation.scenarios, operation.requestSchema)) {
      is ScenarioMatchResult.SingleMatch -> handleScenarioResponse(request, matchResult.scenario, operation)
      is ScenarioMatchResult.NoMatch     -> handleSchemaOnlyResponse(request, operation)
      is ScenarioMatchResult.Ambiguous   ->
        teapotResponse(
          "Ambiguous: multiple scenarios (${matchResult.scenarios.joinToString(", ") { it.key }}) " +
          "matched the request for ${operation.method.uppercase()} ${operation.path}")
    }
  }

  private fun handleScenarioResponse(request: Request, scenario: Scenario, operation: ApiOperation): Response {
    val responseSchema = operation.responseFor(scenario.statusCode)
                         ?: return teapotResponse("No response schema for status ${scenario.statusCode}")

    val acceptResult = verifyAcceptHeader(request.header("Accept"), responseSchema)
    if (acceptResult.isFailure()) return teapotResponse(acceptResult.errors().first())
    return ResponseGenerator.fromScenario(scenario, responseSchema)
  }

  private fun handleSchemaOnlyResponse(request: Request, operation: ApiOperation): Response {
    val unique2xxResult = findUnique2xxResponse(operation)
    if (unique2xxResult.isFailure()) return teapotResponse(unique2xxResult.errors().first())

    val (statusCode, responseSchema) = unique2xxResult.value!!
    val acceptResult = verifyAcceptHeader(request.header("Accept"), responseSchema)
    if (acceptResult.isFailure()) return teapotResponse(acceptResult.errors().first())

    val bodyResult = selectResponseBody(request.header("Accept"), responseSchema, operation)
    if (bodyResult.isFailure()) return teapotResponse(bodyResult.errors().first())

    return ResponseGenerator.fromSchema(statusCode, responseSchema.headers, bodyResult.value)
  }

  private fun findUnique2xxResponse(operation: ApiOperation): Result<Pair<Int, ResponseSchema>> {
    val successResponses = operation.successResponses()
    return when {
      successResponses.isEmpty() ->
        failure("No 2xx response schema defined for ${operation.method.uppercase()} ${operation.path}")
      successResponses.size > 1  ->
        failure(
          "Ambiguous: multiple 2xx response codes (${successResponses.keys.sorted().joinToString(", ")}) " +
          "for ${operation.method.uppercase()} ${operation.path}. Use scenarios to disambiguate.")
      else                       -> success(successResponses.entries.first().toPair())
    }
  }

  private fun verifyAcceptHeader(acceptHeader: String?, responseSchema: ResponseSchema): Result<Unit> {
    val accept = AcceptHeader.parse(acceptHeader)
    if (accept.acceptsAny()) return success()
    if (responseSchema.bodies.isEmpty()) return success()

    if (accept.bestMatch(responseSchema.bodies.map { it.contentType }) == null)
      return failure(
        "Accept header '$acceptHeader' does not match any response content type: " +
        responseSchema.bodies.joinToString(", ") { it.contentType.value })

    return success()
  }

  private fun selectResponseBody(acceptHeader: String?,
                                 responseSchema: ResponseSchema,
                                 operation: ApiOperation): Result<BodySchema?> {
    if (responseSchema.bodies.isEmpty()) return success(null)
    if (responseSchema.bodies.size == 1) return success(responseSchema.bodies.first())

    val accept = AcceptHeader.parse(acceptHeader)
    if (accept.acceptsAny())
      return failure("Multiple response content types for ${operation.method.uppercase()} ${operation.path}. " +
                     "Use Accept header to disambiguate: ${responseSchema.bodies.joinToString(", ") { it.contentType.value }}")

    val bestMatch = accept.bestMatch(responseSchema.bodies.map { it.contentType })
    return if (bestMatch == null)
      failure("Accept header '$acceptHeader' does not match any response content type: " +
              responseSchema.bodies.joinToString(", ") { it.contentType.value })
    else
      success(responseSchema.bodies.find { it.contentType == bestMatch })
  }

  private fun validationErrorResponse(operation: ApiOperation, errors: List<String>): Response =
    Response(I_M_A_TEAPOT)
      .header("Content-Type", TEXT_PLAIN.value)
      .body("Request validation failed for ${operation.method.uppercase()} ${operation.path}:${System.lineSeparator()}" +
            errors.joinToString(System.lineSeparator()) { "  * $it" })

  private fun teapotResponse(message: String): Response =
    Response(I_M_A_TEAPOT)
      .header("Content-Type", TEXT_PLAIN.value)
      .body(message)

  private fun formatRequest(request: Request): String {
    val headers = request.headers.joinToString("\n") { (name, value) -> ">> $name: $value" }
    val body = request.bodyString().ifEmpty { "(none)" }
    return buildString {
      append(">> ${request.method} ${request.uri}")
      if (headers.isNotEmpty()) append("\n$headers")
      append("\n>> Body: $body")
    }
  }

  private fun formatResponse(response: Response): String {
    val status = response.status
    val statusLine = if (status.description.isNotBlank()) "${status.code} ${status.description}" else "${status.code}"
    val headers = response.headers.joinToString("\n") { (name, value) -> "<< $name: $value" }
    val body = response.bodyString().ifEmpty { "(none)" }
    return buildString {
      append("<< $statusLine")
      if (headers.isNotEmpty()) append("\n$headers")
      append("\n<< Body: $body")
    }
  }
}
