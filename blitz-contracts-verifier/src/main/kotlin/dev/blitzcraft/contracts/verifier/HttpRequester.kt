package dev.blitzcraft.contracts.verifier

import dev.blitzcraft.contracts.core.Body
import dev.blitzcraft.contracts.core.Contract
import dev.blitzcraft.contracts.core.Property
import org.http4k.client.JavaHttpClient
import org.http4k.core.*
import org.http4k.core.cookie.cookie

internal class HttpRequester(private val contract: Contract, val serverBaseUri: String, val serverPort: Int) {


  fun sendRequest(): Response {
    val request = Request(
      Method.valueOf(contract.request.method),
      UriTemplate
        .from("$serverBaseUri:$serverPort${contract.request.path}")
        .generate(contract.request.pathParameters.mapValues { it.value.stringValue()!! })
    ).withHeaders(contract.request.headers)
      .withQueryParameters(contract.request.queryParameters)
      .withCookies(contract.request.cookies)
      .withBody(contract.request.body)
      .header("Accept", contract.response.body?.contentType)


    println(request)
    val client: HttpHandler = JavaHttpClient()

    return client(request)
  }

  private fun Request.withHeaders(headers: Map<String, Property>) =
    headers(headers.map { it.key to it.value.stringValue() })

  private fun Request.withQueryParameters(parameters: Map<String, Property>) =
    parameters.entries.fold(this) { req, param -> req.query(param.key, param.value.stringValue()) }

  private fun Request.withCookies(cookies: Map<String, Property>) =
    cookies.entries.fold(this) { req, cookie -> req.cookie(cookie.key, cookie.value.stringValue()!!) }
  private fun Request.withBody(body: Body?): Request {
    return body?.let { header("Content-Type", body.contentType).body(it.asString()) } ?: this
  }
}
