package dev.blitzcraft.contracts.core.loader.swagger

import dev.blitzcraft.contracts.core.contract.*
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem.HttpMethod
import io.swagger.v3.oas.models.responses.ApiResponse

fun extractExampleBasedContracts(context: ContractContext) =
  with(context) {
    val requestExampleKeys = operation.requestExampleKeys()
    val contractExampleKeys = requestExampleKeys intersect apiResponse.exampleKeys()
    val requestsWithExample = lazy {
      requestExampleKeys.associateWith { generateRequests(path, operation, method, it) }
    }

    contractExampleKeys.flatMap { exampleKey ->
      requestsWithExample.value[exampleKey]!!.flatMap { requestContract ->
        generateResponseExamples(exampleKey, statusCode, apiResponse).map { responseContract ->
          Contract(requestContract, responseContract, exampleKey)
        }
      }
    }
  }

private fun generateRequests(path: String,
                             operation: Operation,
                             method: HttpMethod,
                             exampleKey: String): List<ContractRequest> {
  val request = ContractRequest(
    method.name,
    path,
    operation.pathParameters(exampleKey),
    operation.queryParameters(exampleKey),
    operation.headersParameters(exampleKey),
    operation.cookiesParameter(exampleKey)
  )
  return operation.requestBody
           ?.content
           ?.map { (content, mediaType) ->
             request.withBody(Body(content, mediaType.schema.toDataType(), mediaType.contractExample(exampleKey)))
           }
         ?: listOf(request)
}

private fun generateResponseExamples(exampleKey: String, code: Int, apiResponse: ApiResponse): List<ContractResponse> {
  val response = ContractResponse(code, apiResponse.headersParameters(exampleKey))
  return apiResponse.content
           ?.map { (content, mediaType) ->
             response.withBody(Body(content, mediaType.schema.toDataType(), mediaType.contractExample(exampleKey)))
           }
         ?: listOf(response)
}