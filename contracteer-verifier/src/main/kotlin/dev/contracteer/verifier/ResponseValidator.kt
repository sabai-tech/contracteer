package dev.contracteer.verifier

import org.http4k.core.Headers
import org.http4k.core.Response
import dev.contracteer.core.Result
import dev.contracteer.core.Result.Companion.failure
import dev.contracteer.core.Result.Companion.success
import dev.contracteer.core.accumulate
import dev.contracteer.core.operation.BodySchema
import dev.contracteer.core.operation.ParameterElement.Header
import dev.contracteer.core.operation.ParameterSchema
import dev.contracteer.core.operation.ResponseSchema
import dev.contracteer.verifier.VerificationCase.*

private fun Headers.hasHeader(name: String): Boolean =
  any { it.first.equals(name, ignoreCase = true) }

private fun Headers.valuesFor(name: String): List<String> =
  filter { it.first.equals(name, ignoreCase = true) }.mapNotNull { it.second }

private fun Response.contentType(): String? =
  header("Content-Type")

internal object ResponseValidator {
  fun validate(case: VerificationCase, response: Response): Result<Unit> {
    return when (case) {
      is ScenarioBased -> validateResponse(case.scenario.statusCode, case.responseSchema, response)
      is SchemaBased   -> validateResponse(case.statusCode, case.responseSchema, response)
      is TypeMismatch  -> validateResponse(400, case.responseSchema, response)
    }
  }

  private fun validateResponse(expectedStatusCode: Int,
                               responseSchema: ResponseSchema,
                               response: Response): Result<Unit> {
    val statusCodeResult = validateStatusCode(expectedStatusCode, response.status.code)
    return when {
      statusCodeResult.isFailure() -> statusCodeResult
      else                         ->
        validateHeaders(responseSchema.headers, response.headers)
          .andThen { validateBody(responseSchema.bodies, response) }
    }
  }

  private fun validateStatusCode(expected: Int, actual: Int): Result<Unit> {
    return when (expected) {
      actual -> success()
      else   -> failure("Status code does not match. Expected: $expected, Actual: $actual")
    }
  }

  private fun validateHeaders(headerSchemas: List<ParameterSchema>, responseHeaders: Headers): Result<Unit> =
    headerSchemas.accumulate { paramSchema ->
      val element = paramSchema.element as Header
      when {
        !paramSchema.isRequired && !responseHeaders.hasHeader(element.name) -> success()
        paramSchema.isRequired && !responseHeaders.hasHeader(element.name)  -> failure("Response header '${element.name}' is missing")
        else                                                                ->
          paramSchema.codec
            .decode(mapOf(element.name to responseHeaders.valuesFor(element.name)), paramSchema.dataType)
            .flatMap { paramSchema.dataType.validate(it) }
            .forKey(element.name)
      }
    }

  private fun validateBody(bodySchemas: List<BodySchema>,
                           response: Response): Result<Unit> {
    val responseContentType = response.contentType()

    return when {
      bodySchemas.isEmpty() && responseContentType.isNullOrEmpty()    -> success()
      bodySchemas.isEmpty() && !responseContentType.isNullOrEmpty()   -> failure("Expected no Content-Type but found: '$responseContentType'")
      bodySchemas.isNotEmpty() && responseContentType.isNullOrEmpty() -> failure("Content-Type is missing, expected one of: ${bodySchemas.map { it.contentType.value }}")
      else                                                            -> {
        val matchingSchema = bodySchemas.find { it.contentType.validate(responseContentType!!).isSuccess() }
        matchingSchema
          ?.serde
          ?.deserialize(response.bodyString(), matchingSchema.dataType)
          ?.flatMap { matchingSchema.dataType.validate(it) }
          ?.map { }
        ?: failure("Content-Type '$responseContentType' does not match any expected: ${bodySchemas.map { it.contentType.value }}")
      }
    }
  }
}

