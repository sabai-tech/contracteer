package tech.sabai.contracteer.core.swagger

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.Components.COMPONENTS_SCHEMAS_REF
import io.swagger.v3.oas.models.Operation
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

internal fun MediaType.safeExamples() =
  examples ?: example?.let(::singleExampleMap) ?: emptyMap()

internal fun Parameter.safeExamples() =
  examples ?: example?.let(::singleExampleMap) ?: emptyMap()

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

internal fun Schema<*>.safeNullable() =
  nullable ?: false

internal fun <T> Schema<T>.safeEnum() =
  enum ?: emptyList()

internal fun Schema<*>.safeExclusiveMinimum() =
  exclusiveMinimum ?: false

internal fun Schema<*>.safeExclusiveMaximum() =
  exclusiveMaximum ?: false

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

internal fun allAreSuccess(vararg results: Result<*>) =
  results.all { it.isSuccess() }

internal fun parseStatusCode(code: String): Result<Int> =
  code.toIntOrNull()?.let { success(it) } ?: failure("Response status code '$code' is not supported.")
