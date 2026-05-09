package tech.sabai.contracteer.core.swagger

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.Components.COMPONENTS_SCHEMAS_REF
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.SpecVersion.V31
import io.swagger.v3.oas.models.examples.Example
import io.swagger.v3.oas.models.headers.Header
import io.swagger.v3.oas.models.media.Discriminator
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.combineResults
import tech.sabai.contracteer.core.joinWithQuotes
import java.math.BigDecimal

internal fun MediaType.safeExamples() =
  examples ?: example?.let(::singleExampleMap) ?: emptyMap()

internal fun Parameter.safeExamples(): Map<String, Example> =
  examples
    ?: example?.let(::singleExampleMap)
    ?: content?.values?.firstOrNull()?.safeExamples()
    ?: emptyMap()

internal fun Parameter.safeIsRequired() =
  required ?: false

internal fun Parameter.safeAllowReserved() =
  allowReserved ?: false

internal fun Header.safeExamples() =
  examples ?: example?.let(::singleExampleMap) ?: emptyMap()

internal fun Header.safeIsRequired() =
  required ?: false

internal fun ApiResponse.safeHeaders() =
  headers ?: emptyMap()

internal fun Operation.safeParameters() =
  parameters ?: emptyList()

internal fun Schema<*>.safeEnum(): List<Any?> =
  enum ?: emptyList()

internal fun Schema<*>.effectiveEnum(): Result<List<Any?>> {
  val enum = safeEnum()
  val const = effectiveConst() ?: return success(enum)
  return if (enum.all { it == const }) success(listOf(const))
  else failure("Schema '$name': const value '$const' conflicts with enum [${enum.joinWithQuotes()}]")
}

internal fun <T> Schema<*>.mapEnum(transform: (Any) -> Result<T?>): Result<List<T?>> =
  effectiveEnum().flatMap { values ->
    values
      .map { value -> if (value == null) success(null) else transform(value) }
      .combineResults()
  }

internal fun Schema<*>.effectiveType(): String? =
  if (specVersion == V31) types?.singleOrNull { it != "null" }
  else type

internal fun Schema<*>.isNullable(): Boolean =
  if (specVersion == V31) "null" in types.orEmpty()
  else nullable ?: false

internal fun Schema<*>.effectiveLowerBound(): NumericBound =
  if (specVersion == V31) pickMostRestrictive(minimum, exclusiveMinimumValue) { excl, incl -> excl >= incl }
  else NumericBound(minimum, exclusiveMinimum ?: false)

internal fun Schema<*>.effectiveUpperBound(): NumericBound =
  if (specVersion == V31) pickMostRestrictive(maximum, exclusiveMaximumValue) { excl, incl -> excl <= incl }
  else NumericBound(maximum, exclusiveMaximum ?: false)

internal fun Schema<*>.effectiveMinimum(): BigDecimal? = effectiveLowerBound().value
internal fun Schema<*>.effectiveMaximum(): BigDecimal? = effectiveUpperBound().value
internal fun Schema<*>.effectiveExclusiveMinimum(): Boolean = effectiveLowerBound().isExclusive
internal fun Schema<*>.effectiveExclusiveMaximum(): Boolean = effectiveUpperBound().isExclusive

private fun pickMostRestrictive(inclusive: BigDecimal?,
                                exclusive: BigDecimal?,
                                exclusiveWinsOver: (excl: BigDecimal, incl: BigDecimal) -> Boolean): NumericBound =
  when {
    inclusive == null && exclusive == null  -> NumericBound(null, false)
    inclusive == null                       -> NumericBound(exclusive, true)
    exclusive == null                       -> NumericBound(inclusive, false)
    exclusiveWinsOver(exclusive, inclusive) -> NumericBound(exclusive, true)
    else                                    -> NumericBound(inclusive, false)
  }

internal fun Schema<*>.effectiveConst(): Any? =
  if (specVersion == V31) const else null

internal fun Schema<*>.effectiveContentEncoding(): String? =
  if (specVersion == V31) contentEncoding?.lowercase() else null

internal fun Schema<*>.effectiveContentMediaType(): String? =
  if (specVersion == V31) contentMediaType else null

internal fun Schema<*>.isAnyType() =
  effectiveType() == null &&
  properties.isNullOrEmpty() &&
  additionalProperties == null &&
  format == null &&
  maximum == null &&
  minimum == null &&
  exclusiveMaximum == null &&
  exclusiveMinimum == null &&
  pattern == null &&
  minLength == null &&
  maxLength == null &&
  multipleOf == null &&
  default == null &&
  example == null &&
  `enum`.isNullOrEmpty() &&
  effectiveConst() == null

internal fun Schema<*>.shortRef() =
  this.`$ref`?.replace(COMPONENTS_SCHEMAS_REF, "")

internal fun Schema<*>.safeProperties() =
  properties ?: emptyMap()

internal fun Components?.safeSchemas() =
  this?.schemas ?: emptyMap()

internal fun Components?.safeParameters() =
  this?.parameters ?: emptyMap()

internal fun Components?.safeRequestBodies() =
  this?.requestBodies ?: emptyMap()

internal fun Components?.safeExamples() =
  this?.examples ?: emptyMap()

internal fun Components?.safeResponses() =
  this?.responses ?: emptyMap()

internal fun Components?.safeHeaders() =
  this?.headers ?: emptyMap()

internal fun Discriminator.safeMapping() =
  mapping ?: emptyMap()

internal fun RequestBody.safeRequired() =
  required ?: false

internal fun Parameter.shortRef() =
  this.`$ref`?.replace("#/components/parameters/", "")

internal fun RequestBody.shortRef() =
  this.`$ref`?.replace("#/components/requestBodies/", "")

internal fun Header.shortRef() =
  this.`$ref`?.replace("#/components/headers/", "")

internal fun Example.shortRef() =
  this.`$ref`?.replace("#/components/examples/", "")

internal fun ApiResponse.shortRef() =
  this.`$ref`?.replace("#/components/responses/", "")

private fun singleExampleMap(exampleValue: Any) =
  mapOf("_example" to Example().apply { value = exampleValue })

internal fun isClassCode(code: String): Boolean =
  code.length == 3 && code[0].isDigit() && code.substring(1).uppercase() == "XX"

internal fun isBodylessStatusCode(statusCode: Int): Boolean =
  statusCode in 100..199 || statusCode == 204 || statusCode == 205 || statusCode == 304

internal data class NumericBound(val value: BigDecimal?, val isExclusive: Boolean)
