package tech.sabai.contracteer.verifier

import io.github.oshai.kotlinlogging.KotlinLogging
import org.http4k.core.Request
import org.http4k.core.Response

/**
 * Verifies a real server implementation against OpenAPI contract expectations.
 *
 * Sends requests derived from [VerificationCase] instances and validates
 * the server's responses against the expected response schema.
 */
class ServerVerifier(configuration: ServerConfiguration) {
  private val client = VerificationHttpClient("${configuration.baseUrl}:${configuration.port}")
  private val httpLogger = KotlinLogging.logger("tech.sabai.contracteer.http")

  /**
   * Sends a request for the given [case] and validates the response.
   *
   * @return a [VerificationOutcome] containing the case and its validation result
   */
  fun verify(case: VerificationCase): VerificationOutcome {
    val (request, response) = client.execute(case)

    httpLogger.debug { formatRequest(request) }
    httpLogger.debug { formatResponse(response) }

    val validationResult = ResponseValidator.validate(case, response)

    if (validationResult.isFailure()) {
      httpLogger.warn { "Verification failed: ${case.displayName}\n${formatRequest(request)}\n${formatResponse(response)}" }
      httpLogger.warn { "Enable DEBUG logging for 'tech.sabai.contracteer.http' to see all HTTP traffic" }
    }

    return VerificationOutcome(case, validationResult)
  }

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
