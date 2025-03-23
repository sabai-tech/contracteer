package tech.sabai.contracteer.core.swagger

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.Components.COMPONENTS_SCHEMAS_REF
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.headers.Header
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.Discriminator
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.responses.ApiResponse
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.combineResults
import tech.sabai.contracteer.core.contract.Body
import tech.sabai.contracteer.core.contract.ContentType
import tech.sabai.contracteer.core.contract.ContractParameter
import tech.sabai.contracteer.core.swagger.converter.schema.SchemaConverter
import tech.sabai.contracteer.core.swagger.converter.example.ExampleConverter
import tech.sabai.contracteer.core.swagger.converter.parameter.ParameterConverter
import tech.sabai.contracteer.core.swagger.converter.requestbody.RequestBodyConverter

internal fun MediaType.safeExamples() =
  examples ?: emptyMap()

internal fun MediaType.contractExample(exampleKey: String?) =
  if (exampleKey == null || !safeExamples().keys.contains(exampleKey))
    success()
  else
    ExampleConverter.convert(safeExamples()[exampleKey]!!)

internal fun Parameter.safeExamples() =
  examples ?: emptyMap()

internal fun Parameter.safeIsRequired() =
  required ?: false

internal fun Header.safeExamples() =
  examples ?: emptyMap()

internal fun Header.safeIsRequired() =
  required ?: false

internal fun Header.contractExample(exampleKey: String?) =
  if (exampleKey == null || !safeExamples().keys.contains(exampleKey))
    success()
  else
    ExampleConverter.convert(safeExamples()[exampleKey]!!)

internal fun List<Parameter>.exampleKeys() =
  flatMap { it.safeExamples().keys }.toSet()

internal fun Content.exampleKeys() =
  flatMap { it.value.safeExamples().keys }.toSet()

internal fun ApiResponse.safeHeaders() =
  headers ?: emptyMap()

internal fun ApiResponse.exampleKeys() =
  safeHeaders().exampleKeys() + bodyExampleKeys()

internal fun ApiResponse.bodyExampleKeys() =
  content?.exampleKeys() ?: emptySet()

internal fun Operation.safeParameters() =
  parameters ?: emptyList()

internal fun Operation.requestExampleKeys() =
  safeParameters().exampleKeys() + (requestBody?.content?.exampleKeys() ?: emptySet())

internal fun Map<String, Header>.exampleKeys() =
  flatMap { it.value.safeExamples().keys }.toSet()

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

internal fun Discriminator.safeMapping() =
  mapping ?: emptyMap()

internal fun Operation.generatePathParameters(exampleKey: String? = null) =
  safeParameters()
    .filter { it.`in` == "path" }
    .map { if (it.safeIsRequired()) success(it) else failure("Path parameter ${it.name} is required") }
    .combineResults()
    .flatMap { it!!.map { ParameterConverter.convert(it, exampleKey) }.combineResults() }

internal fun Operation.generateQueryParameters(exampleKey: String? = null) =
  safeParameters()
    .filter { it.`in` == "query" }
    .map { ParameterConverter.convert(it, exampleKey) }
    .combineResults()

internal fun Operation.generateRequestHeaders(exampleKey: String? = null) =
  safeParameters()
    .filter { it.`in` == "header" }
    .map { ParameterConverter.convert(it, exampleKey) }
    .combineResults()

internal fun Operation.generateRequestCookies(exampleKey: String? = null) =
  safeParameters()
    .filter { it.`in` == "cookie" }
    .map { ParameterConverter.convert(it, exampleKey) }
    .combineResults()

internal fun Operation.generateRequestBodies(exampleKey: String? = null) =
  if (requestBody == null)
    success(emptyList())
  else
    RequestBodyConverter.convert(this.requestBody!!, exampleKey)


internal fun ApiResponse.generateResponseBodies(exampleKey: String? = null): Result<List<Body>> =
  if (content == null)
    success(emptyList())
  else
    content.map { (contentType, mediaType) ->
      mediaType
        .contractExample(exampleKey)
        .flatMap { resolvedExample ->
          SchemaConverter
            .convertToDataType(mediaType.schema, "")
            .flatMap { Body.create(ContentType(contentType), it!!, resolvedExample) }
        }
    }.combineResults()


internal fun ApiResponse.generateResponseHeaders(exampleKey: String? = null) =
  safeHeaders()
    .map { (name, header) ->
      header.contractExample(exampleKey)
        .flatMap { resolvedExample ->
          SchemaConverter.convertToDataType(header.schema, "")
            .flatMap { ContractParameter.create(name, it!!, header.safeIsRequired(), resolvedExample) }
        }
    }.combineResults()