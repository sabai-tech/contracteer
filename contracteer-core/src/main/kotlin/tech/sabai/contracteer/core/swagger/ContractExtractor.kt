package tech.sabai.contracteer.core.swagger

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.models.OpenAPI
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.combineResults
import tech.sabai.contracteer.core.contract.Contract
import tech.sabai.contracteer.core.datatype.ArrayDataType
import tech.sabai.contracteer.core.datatype.ObjectDataType
import java.lang.System.lineSeparator

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

  if (result.isSuccess()) {
    val contracts = result.value ?: emptyList()
    if (contracts.isEmpty()) {
      logger.warn { "No supported contracts were found." }
    } else {
      logger.info { "${contracts.size} supported contract(s) were found." }
      logger.debug {
        contracts.joinToString("${lineSeparator()}- ", "Contracts:${lineSeparator()}- ") { it.description() }
      }
    }
  } else {
    logger.error {
      "Error generating contracts: " + result.errors().joinToString(lineSeparator(), lineSeparator(), lineSeparator())
    }
  }


  return result
}

private fun removeUnsupportedContracts(contracts: List<Contract>?): List<Contract> =
  contracts?.filter { !it.hasUnsupportedContentType() && !it.hasUnsupportedParameters() } ?: emptyList()

private fun Contract.hasUnsupportedContentType(): Boolean {
  val isUnsupportedResponseContentType = response.body?.contentType?.value == "application/xml"
  if (isUnsupportedResponseContentType)
    logger.warn {
      "Contract '${this.description()}' filtered out: response content-type 'application/xml' is not yet supported."
    }

  val requestContentType = request.body?.contentType?.value
  val isUnsupportedRequestContentType = requestContentType in setOf("multipart/form-data",
                                                                    "application/x-www-form-urlencoded",
                                                                    "application/xml")

  if (isUnsupportedRequestContentType)
    logger.warn { "Contract '${this.description()}' filtered out: request content-type '$requestContentType' is not yet supported." }

  return isUnsupportedResponseContentType || isUnsupportedRequestContentType
}

private fun Contract.hasUnsupportedParameters() =
  (request.pathParameters +
   request.queryParameters +
   request.cookies +
   request.headers +
   response.headers
  ).filter { it.dataType is ObjectDataType || it.dataType is ArrayDataType }
    .onEach { logger.warn { "Contract '${this.description()}' filtered out: parameter '${it.name}': schema 'array' and 'object' are not yet supported." } }
    .isNotEmpty()


