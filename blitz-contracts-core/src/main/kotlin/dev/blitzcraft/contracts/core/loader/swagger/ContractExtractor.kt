package dev.blitzcraft.contracts.core.loader.swagger

import dev.blitzcraft.contracts.core.Result
import dev.blitzcraft.contracts.core.combineResults
import dev.blitzcraft.contracts.core.contract.Contract
import dev.blitzcraft.contracts.core.datatype.ArrayDataType
import dev.blitzcraft.contracts.core.datatype.ObjectDataType
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.models.OpenAPI

private val logger = KotlinLogging.logger {}

internal fun OpenAPI.generateContracts(): Result<List<Contract>> {
  val result = paths.flatMap { (path, item) ->
    item.readOperationsMap().flatMap { (method, operation) ->
      operation.responses.map { (code, response) ->
        SwaggerContext(path, method, operation, code, response).generateContracts()
      }
    }
  }.combineResults()
    .map { (it ?: emptyList()).flatten() }
    .map { removeUnsupportedContracts(it) }

  if (result.isSuccess() && result.value.isNullOrEmpty()) logger.warn { "No supported contracts were found" }

  return result
}

private fun removeUnsupportedContracts(contracts: List<Contract>?): List<Contract> =
  contracts?.filter { !it.hasUnsupportedContentType() && !it.hasUnsupportedParameters() } ?: emptyList()

private fun Contract.hasUnsupportedContentType(): Boolean {
  val isUnsupportedResponseContentType = response.body?.contentType == "application/xml"
  if (isUnsupportedResponseContentType)
    logger.warn { "No contract for ${this.description()} because response content-type 'application/xml' is not yet supported" }

  val isUnsupportedRequestContentType = request.body?.contentType in setOf("multipart/form-data",
                                                                           "application/x-www-form-urlencoded",
                                                                           "application/xml")

  if (isUnsupportedResponseContentType)
    logger.warn { "No contract for ${this.description()} because request content-type 'multipart/form-data', 'application/x-www-form-urlencoded' and 'application/xml' are not yet supported:" }

  return isUnsupportedResponseContentType || isUnsupportedRequestContentType
}

private fun Contract.hasUnsupportedParameters() =
  (request.pathParameters +
   request.queryParameters +
   request.cookies +
   request.headers +
   response.headers
  ).filter { it.dataType is ObjectDataType || it.dataType is ArrayDataType }
    .onEach { logger.warn { "No contract for ${this.description()} because parameter '${it.name}': schema 'array' and 'object' are not yet supported." } }
    .isNotEmpty()


