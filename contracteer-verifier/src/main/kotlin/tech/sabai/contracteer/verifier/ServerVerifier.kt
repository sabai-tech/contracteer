package tech.sabai.contracteer.verifier

import tech.sabai.contracteer.core.contract.Contract
import tech.sabai.contracteer.core.Result

class ServerVerifier(configuration: ServerConfiguration) {
  private val httpRequester = HttpRequester("${configuration.baseUrl}:${configuration.port}")

  fun verify(contract: Contract): Result<Contract> {
    val response = httpRequester.sendRequestFor(contract)
    return ResponseValidator(contract.response).validate(response).map { contract }
  }
}