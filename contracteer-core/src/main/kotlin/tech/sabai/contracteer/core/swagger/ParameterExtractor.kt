package tech.sabai.contracteer.core.swagger

import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.headers.Header
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.responses.ApiResponse
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.codec.ContentCodec
import tech.sabai.contracteer.core.combineResults
import tech.sabai.contracteer.core.datatype.AnyDataType
import tech.sabai.contracteer.core.normalize
import tech.sabai.contracteer.core.operation.ContentType
import tech.sabai.contracteer.core.operation.ParameterElement
import tech.sabai.contracteer.core.operation.ParameterElement.*
import tech.sabai.contracteer.core.operation.ParameterSchema
import tech.sabai.contracteer.core.result
import tech.sabai.contracteer.core.serde.PlainTextSerde
import tech.sabai.contracteer.core.swagger.datatype.DataTypeConverter

internal class ParameterExtractor(
  private val sharedComponents: SharedComponents,
  private val dataTypeConverter: DataTypeConverter,
  private val codecFactory: CodecFactory,
  private val serdeFactory: SerdeFactory
) {

  fun extractPath(operation: Operation): Result<List<ExtractedParameterSchema>> =
    operation.safeParameters()
      .filter { it.`in` == "path" }
      .map { if (it.safeIsRequired()) success(it) else failure("Path parameter ${it.name} is required") }
      .combineResults()
      .flatMap { parameters ->
        parameters
          .onEach { enforceNonEmptyPathParameter(it) }
          .map { it.toParameterSchema(PathParam(it.name)) }
          .combineResults()
      }

  fun extractQuery(operation: Operation): Result<List<ExtractedParameterSchema>> =
    operation.safeParameters()
      .filter { it.`in` == "query" }
      .map { it.toParameterSchema(QueryParam(it.name, it.safeAllowReserved())) }
      .combineResults()

  fun extractRequestHeaders(operation: Operation): Result<List<ExtractedParameterSchema>> =
    operation.safeParameters()
      .filter { it.`in` == "header" && IGNORED_REQUEST_HEADERS.none { h -> h.equals(it.name, ignoreCase = true) } }
      .map { it.toParameterSchema(Header(it.name)) }
      .combineResults()

  fun extractCookies(operation: Operation): Result<List<ExtractedParameterSchema>> =
    operation.safeParameters()
      .filter { it.`in` == "cookie" }
      .map { it.toParameterSchema(Cookie(it.name)) }
      .combineResults()

  fun extractResponseHeaders(response: ApiResponse): Result<List<ExtractedParameterSchema>> =
    response.safeHeaders()
      .filterKeys { !it.equals("Content-Type", ignoreCase = true) }
      .map { (name, header) -> toResponseHeaderSchema(header, name) }
      .combineResults()

  private fun enforceNonEmptyPathParameter(param: Parameter) {
    param.schema
      ?.takeIf { it.type == "string" && (it.minLength == null || it.minLength < 1) }
      ?.apply { minLength = 1 }
  }

  private fun Parameter.toParameterSchema(element: ParameterElement): Result<ExtractedParameterSchema> =
    sharedComponents.resolve(this)
      .flatMap { resolved ->
        if (resolved.content != null && resolved.content.isNotEmpty())
          createContentParameterSchema(resolved, element)
        else
          createStyleParameterSchema(resolved, element)
      }

  private fun createContentParameterSchema(parameter: Parameter,
                                           element: ParameterElement): Result<ExtractedParameterSchema> {
    val (mediaTypeString, mediaTypeObj) = parameter.content.entries.first()
    val contentType = ContentType(mediaTypeString)
    return result {
      val dataType = dataTypeConverter.convertMediaTypeSchema(mediaTypeObj).bind()
      val examples = sharedComponents.resolve(parameter.safeExamples())
        .bind()
        .mapValues { (_, example) -> example.value?.normalize() }

      if (dataType is AnyDataType)
        ExtractedParameterSchema(
          ParameterSchema(element, dataType, parameter.safeIsRequired(), ContentCodec(parameter.name, PlainTextSerde)),
          examples)
      else {
        val serde = serdeFactory.buildSerde(contentType, mediaTypeObj, dataType).bind()
        ExtractedParameterSchema(
          ParameterSchema(element, dataType, parameter.safeIsRequired(), ContentCodec(parameter.name, serde)),
          examples)
      }
    }
  }

  private fun createStyleParameterSchema(parameter: Parameter,
                                         element: ParameterElement): Result<ExtractedParameterSchema> =
    result {
      val dataType = dataTypeConverter.convertToDataType(parameter.schema, "").bind()
      val examples = sharedComponents
        .resolve(parameter.safeExamples())
        .bind()
        .mapValues { (_, example) -> example.value?.normalize() }
      val codec = codecFactory
        .createCodec(element, parameter.style?.toString(), parameter.explode, dataType, parameter.name)
        .bind()

      ExtractedParameterSchema(ParameterSchema(element, dataType, parameter.safeIsRequired(), codec), examples)
    }

  private fun toResponseHeaderSchema(header: Header, name: String): Result<ExtractedParameterSchema> =
    result {
      val resolved = sharedComponents.resolve(header).bind()
      val dataType = dataTypeConverter.convertToDataType(resolved.schema, "").bind()
      val codec = codecFactory
        .createCodec(Header(name), resolved.style?.toString(), resolved.explode, dataType, name)
        .bind()
      val examples = sharedComponents
        .resolve(resolved.safeExamples())
        .bind()
        .mapValues { (_, example) -> example.value?.normalize() }

      ExtractedParameterSchema(ParameterSchema(Header(name), dataType, resolved.safeIsRequired(), codec), examples)
    }

  companion object {
    private val IGNORED_REQUEST_HEADERS = setOf("Accept", "Content-Type", "Authorization")
  }
}
