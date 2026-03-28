package tech.sabai.contracteer.core.swagger

import io.github.oshai.kotlinlogging.KotlinLogging
import tech.sabai.contracteer.core.operation.ApiOperation
import tech.sabai.contracteer.core.operation.BodySchema

private val logger = KotlinLogging.logger {}

internal fun filterUnsupportedOperation(operation: ApiOperation): ApiOperation? {
  val requestBodies = operation.requestSchema.bodies.filterNot { it.isXml() }
  val responses = operation.responses
    .mapValues { (statusCode, schema) ->
      val original = operation.responses[statusCode]!!
      val filtered = schema.copy(bodies = schema.bodies.filterNot { it.isXml() })
      if (original.bodies.isNotEmpty() && filtered.bodies.isEmpty()) null else filtered
    }
    .filterValues { it != null }
    .mapValues { it.value!! }

  if (operation.requestSchema.bodies.isNotEmpty() && requestBodies.isEmpty()) {
    logger.warn { "Operation '${operation.method} ${operation.path}' excluded: XML content type not supported." }
    return null
  }

  if (operation.hasResponses() && responses.isEmpty()) {
    logger.warn { "Operation '${operation.method} ${operation.path}' excluded: all responses use XML content type." }
    return null
  }

  return operation.copy(
    requestSchema = operation.requestSchema.copy(bodies = requestBodies),
    responses = responses
  )
}

private fun BodySchema.isXml() = "xml" in contentType.value.lowercase()