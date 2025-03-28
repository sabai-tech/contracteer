package tech.sabai.contracteer.core.swagger

import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem.HttpMethod
import io.swagger.v3.oas.models.responses.ApiResponse
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.combineResults
import tech.sabai.contracteer.core.contract.Contract
import tech.sabai.contracteer.core.contract.ContractRequest
import tech.sabai.contracteer.core.contract.ContractResponse

data class SwaggerOperationContext(
  val path: String,
  val method: HttpMethod,
  val operation: Operation,
  val statusCode: String,
  val apiResponse: ApiResponse
) {

  fun generateContracts(): Result<List<Contract>> {
    val contractExampleKeys = operation.requestExampleKeys() intersect apiResponse.exampleKeys()
    val contractsFromExamples = contractExampleKeys
      .map { createContracts(it) }
      .combineResults()
      .map { (it ?: emptyList()).flatten() }

    return when {
      contractsFromExamples.isFailure()                                          -> contractsFromExamples
      contractsFromExamples.value.isNullOrEmpty() and statusCode.startsWith("2") -> createContracts()
      else                                                                       -> contractsFromExamples
    }
  }

  private fun createContracts(exampleKey: String? = null): Result<List<Contract>> {
    val requestResults = createRequests(exampleKey).combineResults()
    val responseResults = createResponses(exampleKey).combineResults()

    return if (requestResults.isSuccess() && responseResults.isSuccess())
      requestResults.value!!
        .flatMap { req -> responseResults.value!!.map { res -> success(Contract(req, res, exampleKey)) } }
        .combineResults()
    else
      (requestResults combineWith responseResults.retypeError()).retypeError()
  }

  private fun createRequests(exampleKey: String? = null): List<Result<ContractRequest>> {
    val pathParameters = operation.generatePathParameters(exampleKey).forProperty("path")
    val queryParameters = operation.generateQueryParameters(exampleKey).forProperty("query")
    val headers = operation.generateRequestHeaders(exampleKey).forProperty("header")
    val cookies = operation.generateRequestCookies(exampleKey).forProperty("cookie")
    val bodies = operation.generateRequestBodies(exampleKey).forProperty("body")

    return if (allAreSuccess(pathParameters, queryParameters, cookies, headers, bodies)) {
      val request = ContractRequest(method.name,
                                    path,
                                    pathParameters.value!!,
                                    queryParameters.value!!,
                                    headers.value!!,
                                    cookies.value!!)
      bodies.value!!.map { success(request.withBody(it)) }.ifEmpty { listOf(success(request)) }
    } else {
      listOf(
        pathParameters.mapAndRetypeErrorIfAny(requestErrorMessage(exampleKey)),
        queryParameters.mapAndRetypeErrorIfAny(requestErrorMessage(exampleKey)),
        headers.mapAndRetypeErrorIfAny(requestErrorMessage(exampleKey)),
        cookies.mapAndRetypeErrorIfAny(requestErrorMessage(exampleKey)),
        bodies.mapAndRetypeErrorIfAny(requestErrorMessage(exampleKey))
      )
    }
  }

  private fun createResponses(exampleKey: String? = null): List<Result<ContractResponse>> {
    val headers = apiResponse.generateResponseHeaders(exampleKey).forProperty("header")
    val bodies = apiResponse.generateResponseBodies(exampleKey).forProperty("body")

    return if (allAreSuccess(headers, bodies)) {
      val response = ContractResponse(statusCode.toInt(), headers.value!!)
      bodies.value!!.map { success(response.withBody(it)) }.ifEmpty { listOf(success(response)) }
    } else {
      listOf(
        headers.mapAndRetypeErrorIfAny(responseErrorMessage(exampleKey)),
        bodies.mapAndRetypeErrorIfAny(responseErrorMessage(exampleKey))
      )
    }
  }

  private fun requestErrorMessage(exampleKey: String?): String =
    if (exampleKey != null)
      "$method $path [example: '$exampleKey'] | request"
    else
      "$method $path | request"

  private fun responseErrorMessage(exampleKey: String?): String =
    if (exampleKey != null)
      "$method $path -> $statusCode [example: '$exampleKey' ] | response"
    else
      "$method $path -> $statusCode | response"

  private fun allAreSuccess(vararg results: Result<*>): Boolean =
    results.all { it.isSuccess() }

  private fun <U> Result<Any>.mapAndRetypeErrorIfAny(path: String): Result<U> =
    mapErrors { "$path ▸ $it" }.retypeError()
}
