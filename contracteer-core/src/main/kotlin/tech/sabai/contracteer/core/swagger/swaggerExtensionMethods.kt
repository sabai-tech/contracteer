package tech.sabai.contracteer.core.swagger

import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.headers.Header
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.responses.ApiResponse
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.combineResults
import tech.sabai.contracteer.core.contract.Body
import tech.sabai.contracteer.core.contract.ContractParameter
import tech.sabai.contracteer.core.contract.Example
import tech.sabai.contracteer.core.contract.PathParameter
import tech.sabai.contracteer.core.datatype.ArrayDataType
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.datatype.StructuredObjectDataType
import tech.sabai.contracteer.core.swagger.converter.SchemaConverter


internal fun MediaType.safeExamples() =
  examples ?: emptyMap()

internal fun MediaType.contractExample(exampleKey: String?): Example? =
  exampleKey?.let { safeExamples()[exampleKey]?.let { Example(it.value) } }

internal fun Parameter.safeExamples() =
  examples ?: emptyMap()

internal fun Parameter.contractExample(exampleKey: String?): Example? =
  exampleKey?.let { safeExamples()[exampleKey]?.let { Example(it.value) } }

internal fun Header.safeExamples() =
  examples ?: emptyMap()

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

internal fun Schema<*>.fullyResolve() =
  this.`$ref`?.let { SharedComponents.findSchema(it) } ?: this.apply { name = name ?: "Inline Schema" }

internal fun Operation.generatePathParameters(exampleKey: String? = null) =
  safeParameters()
    .filter { it.`in` == "path" }
    .map { param ->
      // TODO manage param.required
      val example = param.contractExample(exampleKey)
      param.schema.convertToDataType()
        .flatMap { it!!.validateExample(example, param.name) }
        .map { PathParameter(param.name, it!!, example) }
    }.combineResults()

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
  (requestBody?.content?.map { (contentType, mediaType) ->
    val example = mediaType.contractExample(exampleKey)
    mediaType.schema.convertToDataType()
      .flatMap { it!!.validateContentType(contentType) }
      .flatMap { it!!.validateExample(example) }
      .map { Body(contentType, it!!, example) }
  } ?: emptyList()).combineResults()

internal fun ApiResponse.generateResponseBodies(exampleKey: String? = null): Result<List<Body>> {
  return (content?.map { (contentType, mediaType) ->
    val example = mediaType.contractExample(exampleKey)
    mediaType.schema.convertToDataType()
      .flatMap { it!!.validateContentType(contentType) }
      .flatMap { it!!.validateExample(example) }
      .map { Body(contentType, it!!, example) }
  } ?: emptyList()).combineResults()
}

internal fun ApiResponse.generateResponseHeaders(exampleKey: String? = null) =
  safeHeaders().map { (name, header) ->
    val example = header.contractExample(exampleKey)
    header.schema.convertToDataType()
      .flatMap { it!!.validateExample(example, name) }
      .map { ContractParameter(name, it!!, header.required ?: false, example) }
  }.combineResults()

internal fun List<Parameter>.toContractParameters(exampleKey: String?): Result<List<ContractParameter>> =
  map { param ->
    val example = param.contractExample(exampleKey)
    param.schema.convertToDataType()
      .flatMap { it!!.validateExample(example, param.name) }
      .map { ContractParameter(param.name, it!!, param.required ?: false, example) }
  }.combineResults()

private fun Schema<*>.convertToDataType(): Result<DataType<*>> =
  SchemaConverter.convert(this)

private fun DataType<*>.validateExample(example: Example?, propertyName: String? = null): Result<DataType<*>> =
  if (example == null) success(this)
  else validate(example.normalizedValue)
    .let { if (propertyName != null) it.forProperty(propertyName) else it }
    .map { this }

private fun <T> DataType<T>.validateContentType(contentType: String) =
  if (contentType.lowercase().contains("json") && (this !is ArrayDataType && this !is StructuredObjectDataType))
    failure("Content type $contentType supports only 'object' or 'array' schema")
  else
    success(this)

