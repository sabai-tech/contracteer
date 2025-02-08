package tech.sabai.contracteer.verifier

import tech.sabai.contracteer.core.contract.Body
import tech.sabai.contracteer.core.contract.Contract
import tech.sabai.contracteer.core.contract.ContractParameter
import org.http4k.client.JavaHttpClient
import org.http4k.core.*
import org.http4k.core.cookie.cookie

internal class HttpRequester(private val serverBaseUri: String,
                             private val serverPort: Int) {

  fun sendRequestFor(contract: Contract): Response {
    val client: HttpHandler = JavaHttpClient()
    val request =
      Request(method = Method.valueOf(contract.request.method),
              uri = UriTemplate
                .from("$serverBaseUri:$serverPort${contract.request.path}")
                .generate(contract.request.pathParameters.associate { it.name to it.stringValue() })
      ).withHeaders(contract.request.headers)
        .withQueryParameters(contract.request.queryParameters)
        .withCookies(contract.request.cookies)
        .withBody(contract.request.body)

    return client(contract.response.body?.let { request.header("Accept", it.contentType) } ?: request)
  }

  private fun Request.withHeaders(contractHeaders: List<ContractParameter>) =
    headers(contractHeaders.map { it.name to it.stringValue() })

  private fun Request.withQueryParameters(parameters: List<ContractParameter>) =
    parameters.fold(this) { req, param -> req.query(param.name, param.stringValue()) }

  private fun Request.withCookies(cookies: List<ContractParameter>) =
    cookies.fold(this) { req, cookie -> req.cookie(cookie.name, cookie.stringValue()) }

  private fun Request.withBody(body: Body?): Request {
    return body?.let { header("Content-Type", body.contentType).body(it.asString()) } ?: this
  }
}
