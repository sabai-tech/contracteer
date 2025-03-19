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
import tech.sabai.contracteer.core.contract.Example
import tech.sabai.contracteer.core.swagger.converter.SchemaConverter

internal fun MediaType.safeExamples() =
  examples ?: emptyMap()

internal fun MediaType.contractExample(exampleKey: String?): Example? =
  exampleKey?.let { safeExamples()[exampleKey]?.let { Example(it.value) } }

internal fun Parameter.safeExamples() =
  examples ?: emptyMap()

internal fun Parameter.safeIsRequired() =
  required ?: false

internal fun Parameter.contractExample(exampleKey: String?): Example? =
  exampleKey?.let { safeExamples()[exampleKey]?.let { Example(it.value) } }

internal fun Header.safeExamples() =
  examples ?: emptyMap()

internal fun Header.safeIsRequired() =
  required ?: false

internal fun Header.contractExample(exampleKey: String?): Example? =
  exampleKey?.let { safeExamples()[exampleKey]?.let { Example(it.value) } }

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
  properties?:emptyMap()

internal fun Components?.safeSchemas() =
  this?.schemas ?: emptyMap()

internal fun Discriminator.safeMapping() =
  mapping ?: emptyMap()

internal fun Operation.generatePathParameters(exampleKey: String? = null) =
  safeParameters()
    .filter { it.`in` == "path" }
    .map { if (it.safeIsRequired()) success(it) else failure("Path parameter ${it.name} is required") }
    .combineResults()
    .flatMap { it!!.toContractParameters(exampleKey) }

internal fun Operation.generateQueryParameters(exampleKey: String? = null) =
  safeParameters()
    .filter { it.`in` == "query" }
    .toContractParameters(exampleKey)

internal fun Operation.generateRequestHeaders(exampleKey: String? = null) =
  safeParameters()
    .filter { it.`in` == "header" }
    .toContractParameters(exampleKey)

internal fun Operation.generateRequestCookies(exampleKey: String? = null) =
  safeParameters()
    .filter { it.`in` == "cookie" }
    .toContractParameters(exampleKey)

internal fun Operation.generateRequestBodies(exampleKey: String? = null): Result<List<Body>> =
  if (requestBody?.content != null) {
    requestBody.content
      .map { (contentType, mediaType) ->
        SchemaConverter
          .convertToDataType(mediaType.schema, "Request body Inline Schema")
          .flatMap { Body.create(ContentType(contentType), it!!, mediaType.contractExample(exampleKey)) }
      }.combineResults()
  } else success(emptyList())

internal fun ApiResponse.generateResponseBodies(exampleKey: String? = null): Result<List<Body>> =
  if (content != null) {
    content.map { (contentType, mediaType) ->
      SchemaConverter
        .convertToDataType(mediaType.schema, "Response body Inline Schema ")
        .flatMap { Body.create(ContentType(contentType), it!!, mediaType.contractExample(exampleKey)) }
    }.combineResults()
  } else success(emptyList())

internal fun ApiResponse.generateResponseHeaders(exampleKey: String? = null) =
  safeHeaders()
    .map { (name, header) ->
      val example = header.contractExample(exampleKey)
      SchemaConverter.convertToDataType(header.schema, "Response header '$name' Inline Schema ")
        .flatMap { ContractParameter.create(name, it!!, header.safeIsRequired(), example) }
    }.combineResults()

internal fun List<Parameter>.toContractParameters(exampleKey: String?): Result<List<ContractParameter>> =
  map { param ->
    val example = param.contractExample(exampleKey)
    SchemaConverter
      .convertToDataType(param.schema, "Parameter '${param.name}' Inline Schema ")
      .flatMap { ContractParameter.create(param.name, it!!, param.safeIsRequired(), example) }
  }.combineResults()