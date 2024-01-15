package dev.blitzcraft.contracts.core

import dev.blitzcraft.contracts.core.contract.*
import dev.blitzcraft.contracts.core.datatype.toDataType
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.headers.Header
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.responses.ApiResponse

fun OpenAPI.contracts() =
  paths.flatMap { pathAndItem ->
    pathAndItem.item().readOperationsMap().flatMap { methodAndOperation ->
      methodAndOperation.operation().responses.flatMap { codeAndResponse ->
        val contractsWithExample = getContractBasedOnExamples(pathAndItem.path(), methodAndOperation, codeAndResponse)
        if (codeAndResponse.code().startsWith("2") && contractsWithExample.isEmpty())
          defaultSuccessContracts(pathAndItem.path(), methodAndOperation, codeAndResponse)
        else
          contractsWithExample
      }
    }
  }.toSet()

private fun getContractBasedOnExamples(path: String,
                                       methodAndOperation: Map.Entry<PathItem.HttpMethod, Operation>,
                                       codeAndResponse: Map.Entry<String, ApiResponse>): List<Contract> {
  val requestExampleKeys = methodAndOperation.operation().requestExampleKeys()
  val requestsWithExample = requestExampleKeys.associateWith {
    methodAndOperation.operation().generateRequestExamples(path, methodAndOperation.method(), it)
  }
  val contractExampleKeys = requestExampleKeys intersect codeAndResponse.response().exampleKeys()
  val contractsWithExample = contractExampleKeys.flatMap { exampleKey ->
    requestsWithExample[exampleKey]!!.flatMap { requestContract ->
      codeAndResponse.generateResponseExamples(exampleKey).map { responseContract ->
        Contract(requestContract, responseContract, exampleKey)
      }
    }
  }
  return contractsWithExample
}

private fun defaultSuccessContracts(path: String,
                                    methodAndOperation: Map.Entry<PathItem.HttpMethod, Operation>,
                                    codeAndResponse: Map.Entry<String, ApiResponse>): List<Contract> {
  val emptyBodyRequest = ContractRequest(method = methodAndOperation.method().name,
                                         path = path,
                                         pathParameters = methodAndOperation.operation().pathParameters(),
                                         queryParameters = methodAndOperation.operation().queryParameters(),
                                         headers = methodAndOperation.operation().headersParameters(),
                                         cookies = methodAndOperation.operation().cookiesParameter())
  val emptyBodyResponse = ContractResponse(
    headers = codeAndResponse.response().safeHeaders().map { it.toContractParameter() },
    statusCode = codeAndResponse.code().toInt()
  )
  val requests = methodAndOperation.operation().generateRequests(emptyBodyRequest)
  val responses = codeAndResponse.response().generateResponses(emptyBodyResponse)
  return requests.flatMap { request ->
    responses.map { response -> Contract(request, response) }
  }
}

private fun Map.Entry<String, Header>.toContractParameter(exampleKey: String? = null) =
  ContractParameter(
    name = key,
    dataType = value.schema.toDataType(),
    example = exampleKey?.let { value.safeExamples()[exampleKey]?.let { Example(it.value) } },
    isRequired = value.required ?: false
  )

private fun Operation.generateRequests(defaultRequestContract: ContractRequest) =
  requestBody?.content
    ?.map { defaultRequestContract.copy(body = Body(it.key, it.value.schema.toDataType())) }
  ?: listOf(defaultRequestContract)

private fun Operation.pathParameters(exampleKey: String? = null) =
  safeParameters().filter { it.`in` == "path" }.map { it.toPathParameter(exampleKey) }

private fun Operation.queryParameters(exampleKey: String? = null) =
  safeParameters().filter { it.`in` == "query" }.map { it.toContractParameter(exampleKey) }

private fun Operation.headersParameters(exampleKey: String? = null) =
  safeParameters().filter { it.`in` == "header" }.map { it.toContractParameter(exampleKey) }

private fun Operation.cookiesParameter(exampleKey: String? = null) =
  safeParameters().filter { it.`in` == "cookie" }.map { it.toContractParameter(exampleKey) }

private fun Operation.generateRequestExamples(path: String,
                                              method: PathItem.HttpMethod,
                                              exampleKey: String): List<ContractRequest> {
  val emptyBodyRequest = ContractRequest(
    method = method.name,
    path = path,
    pathParameters = pathParameters(exampleKey),
    queryParameters = queryParameters(exampleKey),
    headers = headersParameters(exampleKey),
    cookies = cookiesParameter(exampleKey)
  )
  return requestBody?.content?.map { contentAndMediaType ->
    emptyBodyRequest.copy(
      body = Body(
        contentType = contentAndMediaType.content(),
        dataType = contentAndMediaType.mediaType().schema.toDataType(),
        example = contentAndMediaType.mediaType().safeExamples()[exampleKey]?.let { Example(it.value) }))
  } ?: listOf(emptyBodyRequest)
}

private fun Parameter.toPathParameter(exampleKey: String?) =
  PathParameter(
    name = name,
    dataType = schema.toDataType(),
    example = exampleKey?.let { safeExamples()[exampleKey]?.let { Example(it.value) } },
  )

private fun Parameter.toContractParameter(exampleKey: String?) =
  ContractParameter(
    name = name,
    dataType = schema.toDataType(),
    example = exampleKey?.let { safeExamples()[exampleKey]?.let { Example(it.value) } },
    isRequired = required ?: false
  )

private fun ApiResponse.generateResponses(responseContract: ContractResponse) =
  content?.map { responseContract.copy(body = Body(it.key, it.value.schema.toDataType())) }
  ?: listOf(responseContract)

private fun Map.Entry<String, ApiResponse>.generateResponseExamples(exampleKey: String): List<ContractResponse> {
  val emptyBodyResponse = ContractResponse(
    statusCode = key.toInt(),
    headers = response().safeHeaders().map { it.toContractParameter(exampleKey) }
  )
  return response().content?.map { contentAndMediaType ->
    emptyBodyResponse.copy(
      body = Body(
        contentType = contentAndMediaType.content(),
        dataType = contentAndMediaType.mediaType().schema.toDataType(),
        example = contentAndMediaType.mediaType().safeExamples()[exampleKey]?.let { Example(it.value) }))
  } ?: listOf(emptyBodyResponse)
}

private fun Operation.requestExampleKeys() =
  safeParameters().exampleKeys() + (requestBody?.content?.exampleKeys() ?: emptySet())



