package dev.blitzcraft.contracts.server.verifier

import dev.blitzcraft.contracts.core.Contract
import io.restassured.RestAssured
import io.restassured.response.Response
import io.restassured.specification.RequestSpecification

class HttpRequester(private val contract: Contract) {

  private val requestSpecification: RequestSpecification = RestAssured.given()
    .log().all()
    .basePath(contract.request.path)
    .headers(contract.request.headers.mapValues { it.value.value() })
    .pathParams(contract.request.pathParameters.mapValues { it.value.value() })
    .queryParams(contract.request.queryParameters.mapValues { it.value.value() })
    .cookies(contract.request.cookies.mapValues { it.value.value().toString() })

  fun sendRequest(): Response {
    contract.response.body?.let { requestSpecification.accept(it.contentType) }
    contract.request.body?.let {
      requestSpecification.contentType(it.contentType)
      requestSpecification.body(it.content())
    }
    val response = requestSpecification.request(contract.request.method)
    response.then().log().all()
    return response
  }
}