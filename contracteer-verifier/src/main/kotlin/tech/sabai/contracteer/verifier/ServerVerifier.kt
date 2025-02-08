package tech.sabai.contracteer.verifier

import tech.sabai.contracteer.core.contract.Contract
import tech.sabai.contracteer.core.Result

class ServerVerifier(private val serverBaseUri: String = "http://localhost", val serverPort: Int = 8080) {

  fun verify(contract: Contract): Result<Any?> {
    val httpRequester = HttpRequester(serverBaseUri, serverPort)
    val response = httpRequester.sendRequestFor(contract)
    return ResponseValidator(contract.response).validate(response)
  }
}