package dev.blitzcraft.contracts.server.verifier

import dev.blitzcraft.contracts.core.Contract
import io.restassured.RestAssured

class ServerVerifier(serverBaseUri: String = "http://localhost", serverPort: Int = 8080) {
  init {
    RestAssured.baseURI = serverBaseUri
    RestAssured.port = serverPort
  }

  fun verify(contract: Contract) {
      val response = HttpRequester(contract).sendRequest()
      ResponseAsserter(contract.response).assert(response)
    }
}