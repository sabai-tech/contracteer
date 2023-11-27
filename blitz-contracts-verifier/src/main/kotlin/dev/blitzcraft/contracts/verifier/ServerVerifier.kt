package dev.blitzcraft.contracts.verifier

import dev.blitzcraft.contracts.core.Contract

class ServerVerifier(private val serverBaseUri: String = "http://localhost", val serverPort: Int = 8080) {

  fun verify(contract: Contract) {
      val response = HttpRequester(contract, serverBaseUri, serverPort).sendRequest()
      ResponseAsserter(contract.response).assert(response)
    }
}