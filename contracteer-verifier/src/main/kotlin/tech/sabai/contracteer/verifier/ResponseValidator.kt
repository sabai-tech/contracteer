package tech.sabai.contracteer.verifier

import org.http4k.core.Headers
import org.http4k.core.Response
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.accumulate
import tech.sabai.contracteer.core.operation.BodySchema
import tech.sabai.contracteer.core.operation.ParameterElement.Header
import tech.sabai.contracteer.core.operation.ParameterSchema
import tech.sabai.contracteer.core.operation.ResponseSchema
import tech.sabai.contracteer.verifier.VerificationCase.ScenarioBased
import tech.sabai.contracteer.verifier.VerificationCase.SchemaBased
import tech.sabai.contracteer.verifier.VerificationCase.TypeMismatchCase

private fun Headers.hasHeader(name: String) = any { it.first.equals(name, ignoreCase = true) }
private fun Headers.headerValue(name: String) = find { it.first.equals(name, ignoreCase = true) }?.second
private fun Response.contentType(): String? = header("Content-Type")

internal object ResponseValidator {
  fun validate(case: VerificationCase, response: Response): Result<Unit> {
    return when (case) {
      is ScenarioBased    ->
        validateResponse(case.scenario.statusCode, case.responseSchema, response)
      is SchemaBased      ->
        validateResponse(case.statusCode, case.responseSchema, response)
      is TypeMismatchCase ->
        validateResponse(400, case.responseSchema, response)
    }
  }

  private fun validateResponse(
    expectedStatusCode: Int,
    responseSchema: ResponseSchema,
    response: Response
  ): Result<Unit> {
    return validateStatusCode(expectedStatusCode, response.status.code)
      .andThen { validateHeaders(responseSchema.headers, response.headers) }
      .andThen { validateBody(responseSchema.bodies, response) }
  }

  private fun validateStatusCode(expected: Int, actual: Int): Result<Unit> {
    return if (expected == actual) {
      success()
    } else {
      failure("Status code does not match. Expected: $expected, Actual: $actual")
    }
  }

  private fun validateHeaders(
    headerSchemas: List<ParameterSchema>,
    responseHeaders: Headers
  ): Result<Unit> {
    return headerSchemas.accumulate { paramSchema ->
      val element = paramSchema.element as Header
      when {
        !paramSchema.isRequired && !responseHeaders.hasHeader(element.name) -> success()
        paramSchema.isRequired && !responseHeaders.hasHeader(element.name)  -> failure("Response header '${element.name}' is missing")
        else                                                                ->
          paramSchema.serde
            .deserialize(responseHeaders.headerValue(element.name), paramSchema.dataType)
            .flatMap { paramSchema.dataType.validate(it) }
            .forProperty(element.name)
            .map { }
      }
    }
  }

  private fun validateBody(
    bodySchemas: List<BodySchema>,
    response: Response
  ): Result<Unit> {
    val responseContentType = response.contentType()

    return when {
      bodySchemas.isEmpty() && responseContentType.isNullOrEmpty()    -> success()
      bodySchemas.isEmpty() && !responseContentType.isNullOrEmpty()   -> failure("Expected no Content-Type but found: '$responseContentType'")
      bodySchemas.isNotEmpty() && responseContentType.isNullOrEmpty() -> failure("Content-Type is missing, expected one of: ${bodySchemas.map { it.contentType.value }}")
      else                                                            -> {
        val matchingSchema = bodySchemas.find { it.contentType.validate(responseContentType!!).isSuccess() }

        if (matchingSchema == null) {
          failure("Content-Type '$responseContentType' does not match any expected: ${bodySchemas.map { it.contentType.value }}")
        } else {
          matchingSchema.contentType.serde
            .deserialize(response.bodyString(), matchingSchema.dataType)
            .flatMap { matchingSchema.dataType.validate(it) }
            .map { }
        }
      }
    }
  }

}

