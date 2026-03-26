package tech.sabai.contracteer.mockserver

import org.http4k.core.Request
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.accumulate
import tech.sabai.contracteer.core.operation.BodySchema
import tech.sabai.contracteer.core.operation.RequestSchema

internal fun RequestSchema.validate(request: Request): Result<Unit> =
  parameters.accumulate { paramSchema ->
    val extractor = request.valueExtractorFor(paramSchema.element)
    val result = paramSchema.codec.decode(extractor, paramSchema.dataType)
    when {
      result.isFailure()                             -> result.retypeError<Unit>().forProperty(paramSchema.element.name)
      result.value == null && paramSchema.isRequired -> failure(paramSchema.element.name, "is missing")
      result.value == null                           -> success()
      else                                           ->
        paramSchema.dataType
          .validate(result.value)
          .forProperty(paramSchema.element.name)
          .map {}
    }
  } andThen { bodies.validateBody(request) }

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

  return matchingSchema.serde
    .deserialize(request.bodyString(), matchingSchema.dataType)
    .flatMap { matchingSchema.dataType.validate(it) }
    .mapErrors { "Request body: $it" }
    .map { }
}
