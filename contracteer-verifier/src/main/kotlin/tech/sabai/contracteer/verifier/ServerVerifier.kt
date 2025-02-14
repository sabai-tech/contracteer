package tech.sabai.contracteer.verifier

import tech.sabai.contracteer.core.contract.Contract
import tech.sabai.contracteer.core.Result

class ServerVerifier(configuration: ServerConfiguration) {
  private val serverUrl: String = "${configuration.baseUrl}:${configuration.port}"

  fun verify(contract: Contract): Result<Any?> {
    val httpRequester = HttpRequester(serverUrl)
    val response = httpRequester.sendRequestFor(contract)
    return ResponseValidator(contract.response).validate(response)
  }
}