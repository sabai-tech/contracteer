package tech.sabai.contracteer.mockserver

import org.http4k.core.Request
import org.http4k.core.cookie.cookie
import org.http4k.routing.path
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.accumulate
import tech.sabai.contracteer.core.operation.BodySchema
import tech.sabai.contracteer.core.operation.ParameterElement
import tech.sabai.contracteer.core.operation.ParameterSchema
import tech.sabai.contracteer.core.operation.RequestSchema

internal fun RequestSchema.validate(request: Request): Result<Unit> =
  pathParameters.validate { request.path(it.name) } andThen
    { queryParameters.validate { request.query(it.name) } } andThen
    { headers.validate { request.header(it.name) } } andThen
    { cookies.validate { request.cookie(it.name)?.value } } andThen
    { bodies.validateBody(request) }

private fun List<ParameterSchema>.validate(
  valueExtractor: (ParameterElement) -> String?
): Result<Unit> =
  accumulate { paramSchema ->
    when (val value = valueExtractor(paramSchema.element)) {
      null if paramSchema.isRequired -> failure(paramSchema.element.name, "is missing")
      null                           -> success()
      else                           -> paramSchema.serde
        .deserialize(value, paramSchema.dataType)
        .flatMap { paramSchema.dataType.validate(it) }
        .forProperty(paramSchema.element.name)
        .map {}
    }
  }

private fun List<BodySchema>.validateBody(request: Request): Result<Unit> {
  if (isEmpty()) return success()

  val requestContentType = request.header("Content-Type")
  if (requestContentType.isNullOrEmpty()) {
    return if (any { it.isRequired })
      failure("Request body is required but missing")
    else
      success()
  }

  val matchingSchema = find { it.contentType.validate(requestContentType).isSuccess() }
                       ?: return failure("Request Content-Type '$requestContentType' does not match any expected: ${map { it.contentType.value }}")

  return matchingSchema.contentType.serde
    .deserialize(request.bodyString(), matchingSchema.dataType)
    .flatMap { matchingSchema.dataType.validate(it) }
    .mapErrors { "Request body: $it" }
    .map { }
}
