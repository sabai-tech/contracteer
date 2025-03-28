package tech.sabai.contracteer.verifier

import io.github.oshai.kotlinlogging.KotlinLogging
import org.http4k.client.JavaHttpClient
import org.http4k.core.*
import org.http4k.core.cookie.cookie
import org.http4k.filter.DebuggingFilters
import tech.sabai.contracteer.core.contract.Body
import tech.sabai.contracteer.core.contract.Contract
import tech.sabai.contracteer.core.contract.ContractParameter

internal class HttpRequester(private val serverUrl: String) {
  private val logger = KotlinLogging.logger {}
  private val baseClient = JavaHttpClient()

  fun sendRequestFor(contract: Contract): Response {
    val client =
      if (logger.isDebugEnabled())
        DebuggingFilters.PrintRequestAndResponse().then(baseClient)
      else
        baseClient

    val request = Request(
      method = Method.valueOf(contract.request.method),
      uri = UriTemplate
        .from("$serverUrl${contract.request.path}")
        .generate(contract.request.pathParameters.associate { it.name to it.stringValue() })
    ).withHeaders(contract.request.headers)
      .withQueryParameters(contract.request.queryParameters)
      .withCookies(contract.request.cookies)
      .withBody(contract.request.body)

    return client(contract.response.body?.let { request.header("Accept", it.contentType.value) } ?: request)
  }

  private fun Request.withHeaders(contractHeaders: List<ContractParameter>) =
    headers(contractHeaders.map { it.name to it.stringValue() })

  private fun Request.withQueryParameters(parameters: List<ContractParameter>) =
    parameters.fold(this) { req, param -> req.query(param.name, param.stringValue()) }

  private fun Request.withCookies(cookies: List<ContractParameter>) =
    cookies.fold(this) { req, cookie -> req.cookie(cookie.name, cookie.stringValue()) }

  private fun Request.withBody(body: Body?): Request {
    return body?.let { header("Content-Type", body.contentType.value).body(it.asString()) } ?: this
  }
}
