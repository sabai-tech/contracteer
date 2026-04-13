package tech.sabai.contracteer.mockserver

import org.http4k.core.Request
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.failureForKey
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.Result.Failure
import tech.sabai.contracteer.core.Result.Success
import tech.sabai.contracteer.core.accumulate
import tech.sabai.contracteer.core.datatype.AnyOfDataType
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.datatype.OneOfDataType
import tech.sabai.contracteer.core.operation.BodySchema
import tech.sabai.contracteer.core.operation.RequestSchema

internal fun RequestSchema.validate(request: Request): Result<Unit> =
  parameters.accumulate { paramSchema ->
    val extractor = request.valueExtractorFor(paramSchema.element)
    when (val result = paramSchema.codec.decode(extractor, paramSchema.dataType)) {
      is Failure                                                   ->
        result.forKey(paramSchema.element.name)

      is Success if result.value == null && paramSchema.isRequired ->
        failureForKey(paramSchema.element.name, "is missing")

      is Success if result.value == null                           ->
        success()

      is Success                                                   ->
        paramSchema.dataType.tolerantValidate(result.value).forKey(paramSchema.element.name)
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
    .flatMap { matchingSchema.dataType.tolerantValidate(it) }
    .mapErrors { "Request body: $it" }
    .map { }
}

private fun DataType<out Any>.tolerantValidate(value: Any?): Result<Any?> {
  val result = validate(value)
  if (result.isSuccess()) return result
  if (this !is OneOfDataType || discriminator != null) return result
  val anyOfResult = AnyOfDataType.create(name, subTypes).flatMap { it.validate(value) }
  return if (anyOfResult.isSuccess()) anyOfResult else result
}
