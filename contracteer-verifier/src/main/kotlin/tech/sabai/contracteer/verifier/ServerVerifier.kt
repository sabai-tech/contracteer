package tech.sabai.contracteer.verifier

class ServerVerifier(configuration: ServerConfiguration) {
  private val client = VerificationHttpClient("${configuration.baseUrl}:${configuration.port}")
  
  fun verify(case: VerificationCase): VerificationOutcome {
    val response = client.sendRequest(case)
    val validationResult = ResponseValidator.validate(case, response)
    return VerificationOutcome(case, validationResult)
  }
}
