package tech.sabai.contracteer.verifier

/**
 * Verifies a real server implementation against OpenAPI contract expectations.
 *
 * Sends requests derived from [VerificationCase] instances and validates
 * the server's responses against the expected response schema.
 */
class ServerVerifier(configuration: ServerConfiguration) {
  private val client = VerificationHttpClient("${configuration.baseUrl}:${configuration.port}")

  /**
   * Sends a request for the given [case] and validates the response.
   *
   * @return a [VerificationOutcome] containing the case and its validation result
   */
  fun verify(case: VerificationCase): VerificationOutcome {
    val response = client.sendRequest(case)
    val validationResult = ResponseValidator.validate(case, response)
    return VerificationOutcome(case, validationResult)
  }
}
