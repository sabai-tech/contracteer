package dev.blitzcraft.contracts.mockserver

import dev.blitzcraft.contracts.core.contract.*
import dev.blitzcraft.contracts.core.contract.Body
import dev.blitzcraft.contracts.core.Result
import dev.blitzcraft.contracts.core.Result.Companion.failure
import dev.blitzcraft.contracts.core.Result.Companion.success
import dev.blitzcraft.contracts.core.accumulate
import org.http4k.core.*
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
import org.http4k.core.Status.Companion.CONFLICT
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.cookie.cookie
import org.http4k.filter.DebuggingFilters.PrintRequestAndResponse
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.SunHttp
import org.http4k.server.asServer

class MockServer(private val contracts: List<Contract>,
                 private val port: Int = 0) {
  private lateinit var http4kServer: Http4kServer
  fun start() {
    http4kServer = PrintRequestAndResponse()
      .then(routes(*contracts
        .groupBy { it.request.path to Method.valueOf(it.request.method) }
        .map { it.toRoutingHttpHandler() }
        .toTypedArray()))
      .asServer(SunHttp(port))
      .start()
  }

  fun stop() {
    http4kServer
  }

  fun port(): Int {
    return http4kServer.port()
  }

  private fun Map.Entry<Pair<String, Method>, List<Contract>>.toRoutingHttpHandler() =
    path() bind method() to { request ->
      val matchingContracts = request.matches(contracts())
      when (matchingContracts.countSuccess()) {
        0    -> matchingContracts.toNonMatchingErrorResponse()
        1    -> matchingContracts.firstSuccess().toResponse()
        else -> manageMultipleSuccesses(matchingContracts)
      }
    }

  private fun Request.matches(contracts: List<Contract>) =
    contracts.map { it to it.validate(this) }

  private fun Contract.validate(req: Request) =
    request.pathParameters.verify { req.path(it.name) } andThen
        { request.queryParameters.verify { req.query(it.name) } } andThen
        { request.headers.verify { req.header(it.name) } } andThen
        { request.cookies.verify { req.cookie(it.name)?.value } } andThen
        { (request.body?.verify(req) ?: success()) } andThen
        { verifyAcceptRequestHeader(req.header("Accept")) }

  private fun List<ContractParameter>.verify(parameterValueExtractor: (ContractParameter) -> String?) =
    accumulate {
      val value = parameterValueExtractor.invoke(it)
      when {
        value == null && it.isRequired -> failure(it.name, "is missing")
        value == null                  -> success()
        it.hasExample()                -> value.matchesExample(it)
        else                           -> value.matches(it)
      }
    }

  private fun manageMultipleSuccesses(matchingContracts: List<Pair<Contract, Result<Any?>>>): Response {
    val successfullyMatchingContracts =
      matchingContracts
        .filter { it.result().isSuccess() }
        .map { it.contract() }
        .groupBy { it.priority() }
        .maxBy { it.priority() }
        .value
    return if (successfullyMatchingContracts.size == 1) successfullyMatchingContracts.first().toResponse()
    else successfullyMatchingContracts.toMultipleSuccessMatchingErrorResponse()
  }

  private fun Contract.verifyAcceptRequestHeader(acceptHeaderValue: String?) =
    when {
      response.body == null                                      -> success()
      acceptHeaderValue == null                                  -> failure("Request Header 'Accept' is missing")
      !acceptHeaderValue.startsWith(response.body!!.contentType) -> failure("Request Header 'Accept' does not match: Expected: ${response.body!!.contentType}, actual: $acceptHeaderValue")
      else                                                       -> success(acceptHeaderValue)
    }

  private fun Body.verify(req: Request) =
    when {
      req.contentType() == null                    -> failure("Request Header 'Content-type' is missing")
      !req.contentType()!!.startsWith(contentType) -> failure("Request Header 'Content-type' does not match: Expected: ${contentType}, actual: ${req.contentType()}")
      hasExample()                                 -> req.bodyString().matchesExample(this)
      else                                         -> req.bodyString().matches(this)
    }


  private fun Contract.toResponse(): Response {
    val httpResponse = Response(Status.fromCode(response.statusCode)!!)
    return if (response.hasBody())
      httpResponse.header("Content-type", response.body!!.contentType).body(response.body!!.asString())
    else httpResponse
  }

  private fun List<Pair<Contract, Result<Any?>>>.toNonMatchingErrorResponse() =
    Response(NOT_FOUND)
      .contentType(TEXT_PLAIN)
      .body(
        "Route not found. Closest non matching contracts:${System.lineSeparator()}" + joinToString(System.lineSeparator()) {
          "  - ${it.first.description()}${System.lineSeparator()}" +
          it.second.errors().joinToString(System.lineSeparator()) { err -> "      * $err" }
        }
      )

  private fun List<Contract>.toMultipleSuccessMatchingErrorResponse() =
    Response(CONFLICT)
      .contentType(TEXT_PLAIN)
      .body(
        "Multiple Successfully Matching Contracts:${System.lineSeparator()}" +
        joinToString(System.lineSeparator()) { "  - ${it.description()}" }
      )

  private fun Contract.priority() =
    if (hasExample()) Int.MAX_VALUE else Int.MIN_VALUE // TODO: introduce open api extension to manage priority ?

  private fun Response.contentType(contentType: ContentType) =
    header("Content-type", contentType.value)

  private fun Request.contentType(): String? =
    header("Content-type")

  private fun Map.Entry<Pair<String, Method>, List<Contract>>.path() = key.first
  private fun Map.Entry<Pair<String, Method>, List<Contract>>.method() = key.second
  private fun Map.Entry<Pair<String, Method>, List<Contract>>.contracts() = value
  private fun Map.Entry<Int, List<Contract>>.priority() = key
  private fun List<Pair<Contract, Result<Any?>>>.countSuccess() = filter { it.second.isSuccess() }.size
  private fun List<Pair<Contract, Result<Any?>>>.firstSuccess() = first { it.second.isSuccess() }.first
  private fun Pair<Contract, Result<Any?>>.contract() = first
  private fun Pair<Contract, Result<Any?>>.result() = second
}