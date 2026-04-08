package tech.sabai.contracteer.core.swagger

import io.github.oshai.kotlinlogging.KotlinLogging
import tech.sabai.contracteer.core.codec.ContentCodec
import tech.sabai.contracteer.core.datatype.AnyDataType
import tech.sabai.contracteer.core.operation.ApiOperation
import tech.sabai.contracteer.core.operation.BodySchema
import tech.sabai.contracteer.core.operation.ResponseSchema
import tech.sabai.contracteer.core.operation.Scenario

private val logger = KotlinLogging.logger {}

internal fun filterUnsupportedOperation(operation: ApiOperation): ApiOperation? {
  if (operation.requestSchema.parameters.any { it.dataType is AnyDataType && it.codec is ContentCodec }) {
    logger.warn { "Operation '${operation.method} ${operation.path}' excluded: parameter content has no schema." }
    return null
  }

  val requestBodies = operation.requestSchema.bodies.filterNot { it.isUnsupported() }
  val responses = operation.responses
    .mapValues { (_, schema) -> filterUnsupportedBodies(schema) }
    .filterValues { it != null }
    .mapValues { it.value!! }
  val classResponses = operation.classResponses
    .mapValues { (_, schema) -> filterUnsupportedBodies(schema) }
    .filterValues { it != null }
    .mapValues { it.value!! }
  val defaultResponse = operation.defaultResponse?.let { filterUnsupportedBodies(it) }
  val scenarios = operation.scenarios.filterNot { it.hasXmlContentType() }

  if (operation.requestSchema.bodies.isNotEmpty() && requestBodies.isEmpty()) {
    logger.warn { "Operation '${operation.method} ${operation.path}' excluded: no supported request body content type." }
    return null
  }

  if (operation.hasResponses() && responses.isEmpty()) {
    logger.warn { "Operation '${operation.method} ${operation.path}' excluded: no supported response content type." }
    return null
  }

  return operation.copy(
    requestSchema = operation.requestSchema.copy(bodies = requestBodies),
    responses = responses,
    classResponses = classResponses,
    defaultResponse = defaultResponse,
    scenarios = scenarios
  )
}

private fun filterUnsupportedBodies(schema: ResponseSchema): ResponseSchema? {
  val filtered = schema.copy(bodies = schema.bodies.filterNot { it.isUnsupported() })
  return if (schema.bodies.isNotEmpty() && filtered.bodies.isEmpty()) null else filtered
}

private fun Scenario.hasXmlContentType() =
  request.body?.contentType?.isXml() == true || response.body?.contentType?.isXml() == true

private fun BodySchema.isUnsupported() = contentType.isXml() || dataType is AnyDataType