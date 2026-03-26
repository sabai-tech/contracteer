package tech.sabai.contracteer.core.swagger

import io.github.oshai.kotlinlogging.KotlinLogging
import tech.sabai.contracteer.core.operation.ApiOperation
import tech.sabai.contracteer.core.operation.BodySchema
import tech.sabai.contracteer.core.operation.ResponseSchema

private val logger = KotlinLogging.logger {}

internal fun filterUnsupportedOperation(operation: ApiOperation): ApiOperation? {
  val filteredRequestBodies = filterRequestBodies(operation) ?: return null
  val filteredResponses = filterResponseBodies(operation) ?: return null

  return operation.copy(
    requestSchema = operation.requestSchema.copy(bodies = filteredRequestBodies),
    responses = filteredResponses
  )
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

  return if (operation.hasResponses() && filtered.isEmpty()) null else filtered
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

private fun BodySchema.isUnsupportedResponseContentType() =
  contentType.value == "application/xml"

private fun BodySchema.isUnsupportedRequestContentType(): Boolean {
  val requestContentType = contentType.value
  return requestContentType in setOf("multipart/form-data", "application/xml")
}
