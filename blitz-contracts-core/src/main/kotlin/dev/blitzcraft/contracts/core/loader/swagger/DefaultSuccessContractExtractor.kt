package dev.blitzcraft.contracts.core.loader.swagger

import dev.blitzcraft.contracts.core.contract.Body
import dev.blitzcraft.contracts.core.contract.Contract
import dev.blitzcraft.contracts.core.contract.ContractRequest
import dev.blitzcraft.contracts.core.contract.ContractResponse
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.responses.ApiResponse

fun extractDefaultSuccessContracts(context: ContractContext) =
  with(context) {
    val emptyBodyRequest = ContractRequest(
      method.name,
      path,
      operation.pathParameters(),
      operation.queryParameters(),
      operation.headersParameters(),
      operation.cookiesParameter()
    )
    val emptyBodyResponse = ContractResponse(statusCode, apiResponse.headersParameters())
    val requests = generateRequests(operation, emptyBodyRequest)
    val responses = generateResponses(apiResponse, emptyBodyResponse)

    requests.flatMap { request -> responses.map { response -> Contract(request, response) } }
  }

private fun generateRequests(operation: Operation, request: ContractRequest) =
  operation.requestBody?.content?.map { request.withBody(Body(it.key, it.value.schema.toDataType())) }
  ?: listOf(request)

private fun generateResponses(apiResponse: ApiResponse, response: ContractResponse) =
  apiResponse.content?.map { response.withBody(Body(it.key, it.value.schema.toDataType())) }
  ?: listOf(response)
