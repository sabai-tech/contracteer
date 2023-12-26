package dev.blitzcraft.contracts.verifier

import dev.blitzcraft.contracts.core.Body
import dev.blitzcraft.contracts.core.Contract
import dev.blitzcraft.contracts.core.Property
import org.http4k.client.JavaHttpClient
import org.http4k.core.*
import org.http4k.core.cookie.cookie

internal class HttpRequester(private val contract: Contract, val serverBaseUri: String, val serverPort: Int) {


  fun sendRequest(): Response {
    val client: HttpHandler = JavaHttpClient()
    val request = Request(
      method = Method.valueOf(contract.request.method),
      uri = UriTemplate
        .from("$serverBaseUri:$serverPort${contract.request.path}")
        .generate(contract.request.pathParameters.associate { it.name to it.stringValue()!! })
    ).withHeaders(contract.request.headers)
      .withQueryParameters(contract.request.queryParameters)
      .withCookies(contract.request.cookies)
      .withBody(contract.request.body)
      .header("Accept", contract.response.body?.contentType)

    return client(request)
  }

  private fun Request.withHeaders(headers: List<Property>) = headers(headers.map { it.name to it.stringValue() })

  private fun Request.withQueryParameters(parameters: List<Property>) =
    parameters.fold(this) { req, param -> req.query(param.name, param.stringValue()) }

  private fun Request.withCookies(cookies: List<Property>) =
    cookies.fold(this) { req, cookie -> req.cookie(cookie.name, cookie.stringValue()!!) }

  private fun Request.withBody(body: Body?): Request {
    return body?.let { header("Content-Type", body.contentType).body(it.asString()) } ?: this
  }
}
