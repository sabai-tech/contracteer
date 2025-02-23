package tech.sabai.contracteer.mockserver

import io.github.oshai.kotlinlogging.KotlinLogging
import org.http4k.core.*
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
import org.http4k.core.Status.Companion.I_M_A_TEAPOT
import org.http4k.core.cookie.cookie
import org.http4k.filter.DebuggingFilters.PrintRequestAndResponse
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.accumulate
import tech.sabai.contracteer.core.contract.*
import tech.sabai.contracteer.core.contract.Body

class MockServer(private val contracts: List<Contract>,
                 private val port: Int = 0) {

  private lateinit var http4kServer: Http4kServer
  private val logger = KotlinLogging.logger {}

  fun start() {
    val routeHandlers = contracts
      .groupBy { it.request.path to Method.valueOf(it.request.method) }
      .onEach { logger.info { "Registering route: [${it.key.second}] ${it.key.first} with ${it.value.size} contract(s)" } }
      .map { (pathAndMethod, contracts) -> createRouteHandler(pathAndMethod.first, pathAndMethod.second, contracts) }

    logger.info { "Starting Contracteer mock server" }

    http4kServer = PrintRequestAndResponse()
      .then(routes(*routeHandlers.toTypedArray()))
      .asServer(SunHttp(port))
      .start()
    logger.info { "Contracteer mock server started on port ${this.port()}" }
  }

  fun stop() {
    if (::http4kServer.isInitialized) {
      logger.info { "Stopping Contracteer mock server" }
      http4kServer.stop()
      logger.info { "Stopped Contracteer mock server" }
    }
  }

  fun port(): Int {
    check(::http4kServer.isInitialized) { "Contracteer mock server is not started yet." }
    return http4kServer.port()
  }

  private fun createRouteHandler(path: String, method: Method, matchingContracts: List<Contract>) =
    path bind method to { request ->
      val matchResults = request.matches(matchingContracts)
      when (matchResults.countSuccess()) {
        0    -> matchResults.toNonMatchingErrorResponse()
        1    -> handleSingleSuccess(matchResults.firstSuccess().contract(), request.header("Accept"))
        else -> handleMultipleSuccesses(matchResults, request.header("Accept"))
      }
    }

  private fun handleSingleSuccess(contract: Contract, acceptHeader: String?): Response {
    if (acceptHeader.isNullOrEmpty() || acceptHeader == "*/*") return contract.toResponse()
    return contract
      .verifyAcceptRequestHeader(acceptHeader)
      .let { if (it.isSuccess()) contract.toResponse() else contract.notFoundWithErrors(it.errors()) }
  }

  private fun handleMultipleSuccesses(matchResults: List<Pair<Contract, Result<Any?>>>,
                                      acceptHeader: String?): Response {
    val acceptFiltered = matchResults
      .filter { it.result().isSuccess() }
      .map { it.contract() to it.contract().verifyAcceptRequestHeader(acceptHeader) }
      .filter { it.result().isSuccess() }
      .map { it.contract() }
      .groupBy { it.priority() }
      .maxBy { it.key }
      .value
    return when {
      acceptFiltered.isEmpty() -> matchResults.map { it.contract() }.toMultipleSuccessMatchingErrorResponse()
      acceptFiltered.size == 1 -> acceptFiltered.first().toResponse()
      else                     -> acceptFiltered.toMultipleSuccessMatchingErrorResponse()
    }
  }

  private fun Request.matches(contracts: List<Contract>) =
    contracts.map { it to it.validate(this) }

  private fun Contract.validate(req: Request) =
    request.pathParameters.verify { req.path(it.name) } andThen
        { request.queryParameters.verify { req.query(it.name) } } andThen
        { request.headers.verify { req.header(it.name) } } andThen
        { request.cookies.verify { req.cookie(it.name)?.value } } andThen
        { (request.body?.verify(req) ?: success()) }

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

  private fun Body.verify(req: Request): Result<Any?> {
    val requestContentType = req.contentType() ?: return failure("Request Header 'Content-type' is missing")

    return contentType.validate(requestContentType).flatMap {
      if (hasExample()) req.bodyString().matchesExample(this)
      else req.bodyString().matches(this)
    }.mapErrors { "Request $it" }
  }


  private fun Contract.verifyAcceptRequestHeader(acceptHeader: String?): Result<Contract> =
    if (response.body == null || acceptHeader.isNullOrEmpty() || acceptHeader == "*/*") success()
    else {
      response.body!!.contentType.validate(acceptHeader)
        .map { this }
        .mapErrors { "Request Header 'Accept' does not match: Expected: ${response.body!!.contentType}, actual: $acceptHeader" }
    }

  private fun Contract.priority() =
    if (hasExample()) Int.MAX_VALUE else Int.MIN_VALUE // TODO: introduce open api extension to manage priority ?

  private fun Contract.toResponse() =
    Response(Status.fromCode(response.statusCode)!!).let { baseResponse ->
      if (response.hasBody())
        baseResponse.header("Content-type", response.body!!.contentType.value).body(response.body!!.asString())
      else
        baseResponse
    }

  private fun Contract.notFoundWithErrors(errors: List<String>): Response =
    Response(I_M_A_TEAPOT)
      .contentType(TEXT_PLAIN)
      .body(
        "Route not found. Closest non matching contract:${System.lineSeparator()}" +
        "  - ${description()}${System.lineSeparator()}" +
        errors.joinToString(System.lineSeparator()) { err -> "      * $err" }
      )

  private fun List<Pair<Contract, Result<Any?>>>.toNonMatchingErrorResponse(): Response =
    Response(I_M_A_TEAPOT)
      .contentType(TEXT_PLAIN)
      .body(
        "Route not found. Closest non matching contracts:${System.lineSeparator()}" +
        joinToString(System.lineSeparator()) { (contract, result) ->
          "  - ${contract.description()}${System.lineSeparator()}" +
          result.errors().joinToString(System.lineSeparator()) { err -> "      * $err" }
        }
      )

  private fun List<Contract>.toMultipleSuccessMatchingErrorResponse(): Response =
    Response(I_M_A_TEAPOT)
      .contentType(TEXT_PLAIN)
      .body(
        "Multiple Successfully Matching Contracts:${System.lineSeparator()}" +
        joinToString(System.lineSeparator()) { "  - ${it.description()}" }
      )

  private fun Response.contentType(contentType: ContentType) = header("Content-type", contentType.value)
  private fun Request.contentType(): String? = header("Content-type")
  private fun Pair<Contract, Result<Any?>>.contract() = first
  private fun Pair<Contract, Result<Any?>>.result() = second
  private fun List<Pair<Contract, Result<Any?>>>.countSuccess() = count { it.result().isSuccess() }
  private fun List<Pair<Contract, Result<Any?>>>.firstSuccess() = first { it.result().isSuccess() }
}