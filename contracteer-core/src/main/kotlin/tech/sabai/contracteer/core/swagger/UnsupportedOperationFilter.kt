package tech.sabai.contracteer.core.swagger

import io.github.oshai.kotlinlogging.KotlinLogging
import tech.sabai.contracteer.core.datatype.ArrayDataType
import tech.sabai.contracteer.core.operation.ApiOperation
import tech.sabai.contracteer.core.operation.BodySchema
import tech.sabai.contracteer.core.operation.ResponseSchema

private val logger = KotlinLogging.logger {}

internal fun filterUnsupportedOperation(operation: ApiOperation): ApiOperation? {
  if (operation.hasUnsupportedParameters()) {
    logUnsupportedParameters(operation)
    return null
  }

  val filteredRequestBodies = filterRequestBodies(operation) ?: return null
  val filteredResponses = filterResponseBodies(operation) ?: return null

  return operation.copy(
    requestSchema = operation.requestSchema.copy(bodies = filteredRequestBodies),
    responses = filteredResponses
  )
}

private fun logUnsupportedParameters(operation: ApiOperation) {
  logger.warn {
    "Operation '${operation.method} ${operation.path}' has been excluded: " +
    "parameter schemas of type 'array' or 'object' are not yet supported."
  }
}

private fun filterRequestBodies(operation: ApiOperation): List<BodySchema>? {
  val filtered = operation.requestSchema.bodies.filterNot { it.isUnsupportedRequestContentType() }

  if (operation.requestSchema.bodies.isNotEmpty() && filtered.isEmpty()) {
    val unsupportedTypes = operation.requestSchema.bodies
      .filter { it.isUnsupportedRequestContentType() }
      .map { it.contentType.value }
      .distinct()
      .joinToString(", ")

    logger.warn {
      "Operation '${operation.method} ${operation.path}' has been excluded: " +
      "request content-types not yet supported: $unsupportedTypes."
    }
    return null
  }

  return filtered
}

private fun filterResponseBodies(operation: ApiOperation): Map<Int, ResponseSchema>? {
  val filtered = operation.responses
    .mapNotNull { (statusCode, responseSchema) ->
      filterSingleResponse(operation, statusCode, responseSchema)
    }
    .toMap()

  return if (operation.responses.isNotEmpty() && filtered.isEmpty()) null else filtered
}

private fun filterSingleResponse(
  operation: ApiOperation,
  statusCode: Int,
  responseSchema: ResponseSchema
): Pair<Int, ResponseSchema>? {
  val filteredBodies = responseSchema.bodies.filterNot { it.isUnsupportedResponseContentType() }

  if (responseSchema.bodies.isNotEmpty() && filteredBodies.isEmpty()) {
    val unsupportedTypes = responseSchema.bodies
      .filter { it.isUnsupportedResponseContentType() }
      .map { it.contentType.value }
      .distinct()
      .joinToString(", ")
    logger.warn {
      "Operation '${operation.method.uppercase()} ${operation.path} -> $statusCode' has been excluded: " +
      "response content-types not yet supported: $unsupportedTypes."
    }
    return null
  }

  return statusCode to responseSchema.copy(bodies = filteredBodies)
}

private fun ApiOperation.hasUnsupportedParameters(): Boolean {
  val requestHasUnsupported =
    requestSchema.parameters.any { it.dataType.isFullyStructured() || it.dataType is ArrayDataType }
  val responseHasUnsupported = responses.values
    .flatMap { it.headers }
    .any { it.dataType.isFullyStructured() || it.dataType is ArrayDataType }
  return requestHasUnsupported || responseHasUnsupported
}

private fun BodySchema.isUnsupportedResponseContentType() =
  contentType.value == "application/xml"

private fun BodySchema.isUnsupportedRequestContentType(): Boolean {
  val requestContentType = contentType.value
  return requestContentType in setOf("multipart/form-data", "application/x-www-form-urlencoded", "application/xml")
}
