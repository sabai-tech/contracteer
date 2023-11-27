package dev.blitzcraft.contracts.core

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import dev.blitzcraft.contracts.core.ContractExtractor.extractFrom
import dev.blitzcraft.contracts.core.datatype.DataType
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.headers.Header
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.responses.ApiResponse


fun OpenAPI.contracts() = extractFrom(this)

object ContractExtractor {
  fun extractFrom(openAPI: OpenAPI): Set<Contract> {
    return openAPI.paths.flatMap { pathAndItem ->
      pathAndItem.item().readOperationsMap().flatMap { methodAndOperation ->
        methodAndOperation.operation().responses.flatMap { codeAndResponse ->
          val contractsWithExample = getContractBasedOnExamples(pathAndItem.path(), methodAndOperation, codeAndResponse)
          if (codeAndResponse.code().startsWith("2") && contractsWithExample.isEmpty()) {
            defaultSuccessContracts(pathAndItem.path(), methodAndOperation, codeAndResponse)
          } else {
            contractsWithExample
          }
        }
      }
    }.toSet()
  }

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
    val emptyBodyRequest = RequestContract(method = methodAndOperation.method().name,
                                           path = path,
                                           pathParameters = methodAndOperation.operation().pathParameters(),
                                           queryParameters = methodAndOperation.operation().queryParameters(),
                                           headers = methodAndOperation.operation().headersParameters(),
                                           cookies = methodAndOperation.operation().cookiesParameter())
    val emptyBodyResponse = ResponseContract(
      headers = codeAndResponse.response().safeHeaders().mapValues { it.value.toProperty() },
      statusCode = codeAndResponse.code().toInt()
    )
    val requests = methodAndOperation.operation().generateRequests(emptyBodyRequest)
    val responses = codeAndResponse.response().generateResponses(emptyBodyResponse)
    return requests.flatMap { request ->
      responses.map { response -> Contract(request, response) }
    }
  }
}

private fun convert(value: Any?) = when (value) {
  is ObjectNode -> ObjectMapper().convertValue(value, Map::class.java)
  is ArrayNode  -> ObjectMapper().convertValue(value, Array::class.java)
  else          -> value
}

private fun Header.toProperty(exampleKey: String? = null) =
  Property(DataType.from(schema), exampleKey?.let { safeExamples()[exampleKey]?.let { Example(it.value) } })

private fun Map<String, Header>.exampleKeys() = flatMap { it.value.safeExamples().keys }.toSet()

private fun Operation.requestExampleKeys() =
  safeParameters().exampleKeys() + (requestBody?.content?.exampleKeys() ?: emptySet())

private fun Operation.generateRequests(defaultRequestContract: RequestContract) =
  requestBody?.content?.map {
    defaultRequestContract.copy(body = Body(it.key, DataType.from(it.value.schema)))
  } ?: listOf(defaultRequestContract)

private fun Operation.pathParameters(exampleKey: String? = null) =
  safeParameters().filter { it.`in` == "path" }.associate { it.name to it.toProperty(exampleKey) }

private fun Operation.queryParameters(exampleKey: String? = null) =
  safeParameters().filter { it.`in` == "query" }.associate { it.name to it.toProperty(exampleKey) }

private fun Operation.headersParameters(exampleKey: String? = null) =
  safeParameters().filter { it.`in` == "header" }.associate { it.name to it.toProperty(exampleKey) }

private fun Operation.cookiesParameter(exampleKey: String? = null) =
  safeParameters().filter { it.`in` == "cookie" }.associate { it.name to it.toProperty(exampleKey) }

private fun Operation.generateRequestExamples(path: String,
                                              method: PathItem.HttpMethod,
                                              exampleKey: String): List<RequestContract> {
  val emptyBodyRequest = RequestContract(
    method = method.name,
    path = path,
    pathParameters = pathParameters(exampleKey),
    queryParameters = queryParameters(exampleKey),
    headers = headersParameters(exampleKey),
    cookies = cookiesParameter(exampleKey)
  )
  return requestBody?.content?.map { contentAndMediaType ->
    emptyBodyRequest.copy(
      body = Body(contentType = contentAndMediaType.key,
                  dataType = DataType.from(contentAndMediaType.value.schema),
                  example = contentAndMediaType.value.safeExamples()[exampleKey]?.let { Example(convert(it.value)) }))
  } ?: listOf(emptyBodyRequest)
}

private fun Parameter.toProperty(exampleKey: String?) =
  Property(DataType.from(schema), exampleKey?.let { safeExamples()[exampleKey]?.let { Example(it.value) } })

private fun List<Parameter>.exampleKeys() = flatMap { it.safeExamples().keys }.toSet()

private fun Content.exampleKeys() = flatMap { it.value.safeExamples().keys }.toSet()

private fun ApiResponse.generateResponses(responseContract: ResponseContract) =
  content?.map { responseContract.copy(body = Body(it.key, DataType.from(it.value.schema))) }
  ?: listOf(responseContract)
private fun ApiResponse.exampleKeys() = safeHeaders().exampleKeys() + bodyExampleKeys()
private fun ApiResponse.bodyExampleKeys() = content?.exampleKeys() ?: emptySet()
private fun Map.Entry<String, ApiResponse>.generateResponseExamples(exampleKey: String): List<ResponseContract> {
  val emptyBodyResponse = ResponseContract(
    statusCode = key.toInt(),
    headers = response().safeHeaders().mapValues { it.value.toProperty(exampleKey) }
  )
  return response().content?.map { contentAndMediaType ->
    emptyBodyResponse.copy(
      body = Body(
        contentType = contentAndMediaType.key,
        dataType = DataType.from(contentAndMediaType.value.schema),
        example = contentAndMediaType.value.safeExamples()[exampleKey]?.let { Example(convert(it.value)) }))
  } ?: listOf(emptyBodyResponse)
}



