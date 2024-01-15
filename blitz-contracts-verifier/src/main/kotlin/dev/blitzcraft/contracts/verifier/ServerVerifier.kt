package dev.blitzcraft.contracts.verifier

import dev.blitzcraft.contracts.core.contract.Contract
import dev.blitzcraft.contracts.core.validation.ValidationResult

class ServerVerifier(private val serverBaseUri: String = "http://localhost", val serverPort: Int = 8080) {

  fun verify(contract: Contract): ValidationResult {
    val response = HttpRequester(contract, serverBaseUri, serverPort).sendRequest()
    return ResponseValidator(contract.response).validate(response)
  }
}