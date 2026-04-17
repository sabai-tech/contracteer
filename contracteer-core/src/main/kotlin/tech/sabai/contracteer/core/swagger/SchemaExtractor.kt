package tech.sabai.contracteer.core.swagger

import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.examples.Example
import io.swagger.v3.oas.models.headers.Header
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.failureForKey
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.Result.Success
import tech.sabai.contracteer.core.accumulate
import tech.sabai.contracteer.core.codec.*
import tech.sabai.contracteer.core.combineResults
import tech.sabai.contracteer.core.datatype.*
import tech.sabai.contracteer.core.normalize
import tech.sabai.contracteer.core.operation.BodySchema
import tech.sabai.contracteer.core.operation.ContentType
import tech.sabai.contracteer.core.operation.ParameterElement
import tech.sabai.contracteer.core.operation.ParameterElement.*
import tech.sabai.contracteer.core.operation.ParameterSchema
import tech.sabai.contracteer.core.serde.*
import tech.sabai.contracteer.core.swagger.datatype.DataTypeConverter

internal class SchemaExtractor(
  private val sharedComponents: SharedComponents,
  private val dataTypeConverter: DataTypeConverter
) {

  fun extractRequestSchema(operation: Operation): Result<ExtractedRequestSchema> {
    val pathParameters = extractPathParameterSchemas(operation).forProperty("request.path")
    val queryParameters = extractQueryParameterSchemas(operation).forProperty("request.query")
    val headers = extractRequestHeaderSchemas(operation).forProperty("request.header")
    val cookies = extractRequestCookieSchemas(operation).forProperty("request.cookie")
    val bodies = extractRequestBodySchemas(operation).forProperty("request.body")

    return if (pathParameters is Success && queryParameters is Success && headers is Success && cookies is Success && bodies is Success)
      success(
        ExtractedRequestSchema(
          parameters = pathParameters.value + queryParameters.value + headers.value + cookies.value,
          bodies = bodies.value
        ))
    else
      (pathParameters combineWith queryParameters combineWith headers combineWith cookies combineWith bodies).retypeError()
  }

  fun extractResponseSchema(code: String, response: ApiResponse): Result<Pair<Int, ExtractedResponseSchema>> =
    parseStatusCode(code).flatMap { statusCode ->
      sharedComponents
        .resolve(response)
        .flatMap { resolved ->
          rejectBodylessResponseWithBody(statusCode, resolved)
            .flatMap { buildResponseSchema(code, resolved) }
            .map { statusCode to it }
        }
    }

  private fun rejectBodylessResponseWithBody(statusCode: Int, response: ApiResponse): Result<Unit> =
    if (isBodylessStatusCode(statusCode) && !response.content.isNullOrEmpty())
      failure("Response $statusCode declares a body, but HTTP $statusCode MUST NOT include a message body (RFC 7231)")
    else
      success()

  private fun buildResponseSchema(code: String, response: ApiResponse): Result<ExtractedResponseSchema> {
    val headers = extractResponseHeaderSchemas(response)
    val bodies = extractResponseBodySchemas(response)
    return if (headers is Success && bodies is Success)
      success(ExtractedResponseSchema(headers.value, bodies.value))
    else
      (headers combineWith bodies).forKey(code).forProperty("response").retypeError()
  }

  fun extractResponseSchema(response: ApiResponse): Result<ExtractedResponseSchema> =
    sharedComponents
      .resolve(response)
      .flatMap { resolved ->
        val headers = extractResponseHeaderSchemas(resolved)
        val bodies = extractResponseBodySchemas(resolved)
        if (headers is Success && bodies is Success)
          success(ExtractedResponseSchema(headers.value, bodies.value))
        else
          (headers combineWith bodies).retypeError()
      }

  private fun extractPathParameterSchemas(operation: Operation): Result<List<ExtractedParameterSchema>> =
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

  private fun enforceNonEmptyPathParameter(param: Parameter) {
    val schema = param.schema ?: return
    if (schema.type == "string" && (schema.minLength == null || schema.minLength < 1))
      schema.minLength = 1
  }

  private fun extractQueryParameterSchemas(operation: Operation): Result<List<ExtractedParameterSchema>> =
    operation.safeParameters()
      .filter { it.`in` == "query" }
      .map { param ->
        sharedComponents
          .resolve(param)
          .flatMap { resolved ->
            resolved.toParameterSchema(QueryParam(resolved.name, resolved.safeAllowReserved()))
          }
      }
      .combineResults()

  private fun extractRequestHeaderSchemas(operation: Operation): Result<List<ExtractedParameterSchema>> =
    operation.safeParameters()
      .filter { it.`in` == "header" && IGNORED_REQUEST_HEADERS.none { h -> h.equals(it.name, ignoreCase = true) } }
      .map { it.toParameterSchema(Header(it.name)) }
      .combineResults()

  private fun extractRequestCookieSchemas(operation: Operation): Result<List<ExtractedParameterSchema>> =
    operation.safeParameters()
      .filter { it.`in` == "cookie" }
      .map { it.toParameterSchema(Cookie(it.name)) }
      .combineResults()

  private fun extractRequestBodySchemas(operation: Operation): Result<List<ExtractedBodySchema>> =
    if (operation.requestBody == null)
      success(emptyList())
    else
      extractRequestBodySchemas(operation.requestBody!!)

  private fun extractRequestBodySchemas(requestBody: RequestBody): Result<List<ExtractedBodySchema>> =
    sharedComponents.resolve(requestBody).flatMap { convertRequestBodySchema(it) }

  private fun convertRequestBodySchema(body: RequestBody): Result<List<ExtractedBodySchema>> =
    if (body.content == null)
      success(emptyList())
    else {
      val multiContent = body.content.size > 1
      body.content
        .map { ContentType(it.key) to it.value!! }
        .map { (contentType, mediaType) ->
          convertDataType(mediaType)
            .flatMap { dataType ->
              resolveExamples(mediaType.safeExamples())
                .flatMap { examples ->
                  if (dataType is AnyDataType)
                    success(ExtractedBodySchema(BodySchema(contentType, dataType, body.safeRequired(), PlainTextSerde),
                                                examples))
                  else
                    buildSerde(contentType, mediaType, dataType)
                      .map {
                        ExtractedBodySchema(BodySchema(contentType, dataType.asRequestType(), body.safeRequired(), it),
                                            examples)
                      }
                }
            }.let { if (multiContent) it.forKey(contentType.value) else it }
        }.combineResults()
    }

  private fun extractResponseHeaderSchemas(response: ApiResponse): Result<List<ExtractedParameterSchema>> =
    response
      .safeHeaders()
      .filterKeys { !it.equals("Content-Type", ignoreCase = true) }
      .map { (name, header) -> header.toParameterSchema(name) }
      .combineResults()
      .forProperty("header")

  private fun extractResponseBodySchemas(response: ApiResponse): Result<List<ExtractedBodySchema>> =
    if (response.content == null)
      success(emptyList())
    else {
      val multiContent = response.content.size > 1
      response.content
        .map { ContentType(it.key) to it.value!! }
        .map { (contentType, mediaType) ->
          convertDataType(mediaType).flatMap { dataType ->
            resolveExamples(mediaType.safeExamples()).flatMap { examples ->
              if (dataType is AnyDataType)
                success(ExtractedBodySchema(BodySchema(contentType, dataType, false, PlainTextSerde), examples))
              else
                buildSerde(contentType, mediaType, dataType)
                  .map {
                    ExtractedBodySchema(BodySchema(contentType,
                                                   dataType.asResponseType(),
                                                   mediaType.schema.safeNullable(),
                                                   it), examples)
                  }
            }
          }.let { if (multiContent) it.forKey(contentType.value) else it }
        }.combineResults()
        .forProperty("body")
    }

  private fun convertDataType(mediaType: MediaType): Result<DataType<out Any>> =
    if (mediaType.schema == null) success(AnyDataType)
    else dataTypeConverter.convertToDataType(mediaType.schema, "")

  private fun buildSerde(contentType: ContentType,
                         mediaType: MediaType,
                         dataType: DataType<out Any>): Result<Serde> =
    when {
      contentType.isFormUrlEncoded() && dataType !is ObjectDataType                       ->
        failure("Content type application/x-www-form-urlencoded requires object schema")

      contentType.isMultipart() && dataType !is ObjectDataType                            ->
        failure("Content type ${contentType.value} requires object schema")

      contentType.isFormUrlEncoded()                                                      ->
        buildFormUrlEncodedSerde(dataType as ObjectDataType, mediaType)

      contentType.isMultipart()                                                           ->
        buildMultipartSerde(dataType as ObjectDataType, mediaType)

      contentType.isJson()                                                                ->
        success(JsonSerde)

      !contentType.isXml() && (dataType.isFullyStructured() || dataType is ArrayDataType) ->
        failure("Content type ${contentType.value} supports only primitive schemas (string, integer, number, boolean and their formats)")

      else                                                                                ->
        success(PlainTextSerde)
    }

  private fun buildFormUrlEncodedSerde(dataType: ObjectDataType, mediaType: MediaType): Result<Serde> {
    val nestedTypeErrors = validateFormUrlEncodedProperties(dataType)
    if (nestedTypeErrors.isFailure()) return nestedTypeErrors.retypeError()

    val encodingMap = mediaType.encoding ?: emptyMap()
    return dataType.properties
      .map { (name, type) ->
        val encoding = encodingMap[name]
        val allowReserved = encoding?.allowReserved == true
        createCodecForParameter(QueryParam(name), encoding?.style?.toString(), encoding?.explode, type, name)
          .map { name to FormUrlEncodedSerde.PropertyEncoding(it, allowReserved) }
      }
      .combineResults()
      .map<Serde> { FormUrlEncodedSerde(it.toMap()) }
  }

  private fun validateFormUrlEncodedProperties(dataType: ObjectDataType): Result<Unit> =
    dataType.properties.entries.accumulate { (name, type) ->
      when (type) {
        is ObjectDataType                                      ->
          failure(name,
                  "Form-urlencoded does not support nested object properties (undefined behavior in the OpenAPI specification)")

        is ArrayDataType if type.itemDataType.isNonPrimitive() ->
          failure(name,
                  "Form-urlencoded does not support arrays of complex types (undefined behavior in the OpenAPI specification)")

        else                                                   ->
          success()
      }
    }

  private fun buildMultipartSerde(dataType: ObjectDataType, mediaType: MediaType): Result<Serde> {
    val encodingMap = mediaType.encoding ?: emptyMap()
    val partConfigs = dataType.properties.map { (name, propType) ->
      val contentType = encodingMap[name]?.contentType ?: defaultPartContentType(propType)
      val isFile = propType.isBinary()
      val isFileArray = propType is ArrayDataType && propType.itemDataType.isBinary()
      name to PartConfig(contentType, serdeForContentType(contentType), isFile || isFileArray, isFileArray)
    }.toMap()
    return success(MultipartSerde(partConfigs))
  }

  private fun defaultPartContentType(dataType: DataType<out Any>): String = when {
    dataType.isBinary()                                           -> "application/octet-stream"
    dataType is ArrayDataType && dataType.itemDataType.isBinary() -> "application/octet-stream"
    dataType is ArrayDataType || dataType.isFullyStructured()     -> "application/json"
    else                                                          -> "text/plain"
  }

  private fun serdeForContentType(contentType: String): Serde =
    if ("json" in contentType.lowercase()) JsonSerde else PlainTextSerde

  private fun Parameter.toParameterSchema(element: ParameterElement): Result<ExtractedParameterSchema> =
    sharedComponents.resolve(this).flatMap { resolved ->
      if (resolved.content != null && resolved.content.isNotEmpty())
        createContentParameterSchema(resolved, element)
      else
        createStyleParameterSchema(resolved, element)
    }

  private fun createContentParameterSchema(parameter: Parameter,
                                           element: ParameterElement): Result<ExtractedParameterSchema> {
    val (mediaTypeString, mediaTypeObj) = parameter.content.entries.first()
    val contentType = ContentType(mediaTypeString)
    return convertDataType(mediaTypeObj).flatMap { dataType ->
      resolveExamples(parameter.safeExamples()).flatMap { examples ->
        if (dataType is AnyDataType)
          success(ExtractedParameterSchema(
            ParameterSchema(element,
                            dataType,
                            parameter.safeIsRequired(),
                            ContentCodec(parameter.name, PlainTextSerde)),
            examples))
        else
          buildSerde(contentType, mediaTypeObj, dataType)
            .map {
              ExtractedParameterSchema(
                ParameterSchema(element, dataType, parameter.safeIsRequired(), ContentCodec(parameter.name, it)),
                examples)
            }
      }
    }
  }

  private fun createStyleParameterSchema(parameter: Parameter,
                                         element: ParameterElement): Result<ExtractedParameterSchema> =
    dataTypeConverter
      .convertToDataType(parameter.schema, "")
      .flatMap { dataType ->
        createCodecForParameter(element, parameter.style?.toString(), parameter.explode, dataType, parameter.name)
          .flatMap { codec ->
            resolveExamples(parameter.safeExamples())
              .map { examples ->
                ExtractedParameterSchema(ParameterSchema(element,
                                                         dataType,
                                                         parameter.safeIsRequired(),
                                                         codec), examples)
              }
          }
      }

  private fun createCodecForParameter(element: ParameterElement,
                                      style: String?,
                                      explode: Boolean?,
                                      dataType: DataType<out Any>,
                                      paramName: String) =
    resolveStyle(element, style, explode, paramName).flatMap { (actualStyle, actualExplode) ->
      val validation = validateStyleConstraints(actualStyle, actualExplode, dataType, paramName)
      if (validation.isFailure())
        validation.retypeError()
      else
        when (actualStyle) {
          "simple"         -> success(SimpleParameterCodec(paramName, actualExplode))
          "form"           -> success(FormParameterCodec(paramName, actualExplode))
          "label"          -> success(LabelParameterCodec(paramName, actualExplode))
          "matrix"         -> success(MatrixParameterCodec(paramName, actualExplode))
          "spacedelimited" -> success(SpaceDelimitedParameterCodec(paramName))
          "pipedelimited"  -> success(PipeDelimitedParameterCodec(paramName))
          "deepobject"     -> success(DeepObjectParameterCodec(paramName))
          else             -> failureForKey(paramName, "Unknown style '$actualStyle'")
        }
    }

  private fun resolveStyle(element: ParameterElement,
                           style: String?,
                           explode: Boolean?,
                           paramName: String): Result<Pair<String, Boolean>> {
    val normalizedStyle = style?.lowercase()?.replace("_", "")
    val (defaultStyle, defaultExplode, supportedStyles, locationName) = when (element) {
      is PathParam               -> StyleDefaults("simple", false, setOf("simple", "label", "matrix"), "path")
      is QueryParam              -> StyleDefaults("form", true, setOf("form", "spacedelimited", "pipedelimited", "deepobject"), "query")
      is ParameterElement.Header -> StyleDefaults("simple", false, setOf("simple"), "header")
      is Cookie                  -> StyleDefaults("form", true, setOf("form"), "cookie")
    }
    val actualStyle = normalizedStyle ?: defaultStyle

    return when (actualStyle) {
      in supportedStyles -> success(actualStyle to (explode ?: defaultExplode))
      else               ->
        failureForKey(paramName, "Style '${style ?: actualStyle}' is not supported for $locationName parameters")
    }
  }

  private fun validateStyleConstraints(style: String,
                                       explode: Boolean,
                                       dataType: DataType<out Any>,
                                       paramName: String) =
    when (style) {
      "simple", "form",
      "label", "matrix" -> validateFlatObjectProperties(style, dataType, paramName)
      "deepobject"      -> validateDeepObjectParameters(dataType, paramName, explode)
      "spacedelimited"  -> validateSpaceDelimitedParameters(dataType, paramName, explode)
      "pipedelimited"   -> validatePipeDelimitedParameters(dataType, paramName, explode)
      else              -> success()
    }

  private fun validateFlatObjectProperties(style: String, dataType: DataType<out Any>, paramName: String) =
    if (dataType is ObjectDataType && dataType.hasNonPrimitiveProperties())
      failureForKey(paramName,
                    "Style '$style' does not support objects with nested objects or arrays in properties (undefined behavior in the OpenAPI specification)")
    else
      success()

  private fun validateDeepObjectParameters(dataType: DataType<out Any>, paramName: String, explode: Boolean) =
    when {
      dataType !is ObjectDataType          -> failureForKey(paramName, "Style 'deepObject' requires object type")
      !explode                             -> failureForKey(paramName, "Style 'deepObject' requires explode=true")
      dataType.hasNonPrimitiveProperties() -> failureForKey(paramName,
                                                            "Style 'deepObject' does not support nested objects or arrays in properties (undefined behavior in the OpenAPI specification)")
      else                                 -> success()
    }

  private fun validatePipeDelimitedParameters(dataType: DataType<out Any>, paramName: String, explode: Boolean) =
    when {
      dataType !is ArrayDataType -> failureForKey(paramName, "Style 'pipeDelimited' requires array type")
      explode                    -> failureForKey(paramName, "Style 'pipeDelimited' requires explode=false")
      else                       -> success()
    }

  private fun validateSpaceDelimitedParameters(dataType: DataType<out Any>, paramName: String, explode: Boolean) =
    when {
      dataType !is ArrayDataType -> failureForKey(paramName, "Style 'spaceDelimited' requires array type")
      explode                    -> failureForKey(paramName, "Style 'spaceDelimited' requires explode=false")
      else                       -> success()
    }

  private fun parseStatusCode(code: String): Result<Int> =
    code.toIntOrNull()?.let { success(it) } ?: failure("Response status code '$code' is not supported.")

  private fun ObjectDataType.hasNonPrimitiveProperties(): Boolean =
    properties.values.any { it.isNonPrimitive() }

  private fun DataType<out Any>.isNonPrimitive(): Boolean =
    isFullyStructured() || this is ArrayDataType

  private fun DataType<out Any>.isBinary() = this is BinaryDataType || this is Base64DataType

  private fun Header.toParameterSchema(name: String): Result<ExtractedParameterSchema> =
    sharedComponents.resolve(this).flatMap { resolved ->
      dataTypeConverter
        .convertToDataType(resolved.schema, "")
        .flatMap { dataType ->
          createCodecForParameter(Header(name), resolved.style?.toString(), resolved.explode, dataType, name)
            .flatMap { codec ->
              resolveExamples(resolved.safeExamples())
                .map { examples ->
                  ExtractedParameterSchema(ParameterSchema(Header(name),
                                                           dataType,
                                                           resolved.safeIsRequired(),
                                                           codec), examples)
                }
            }
        }
    }

  private fun resolveExamples(examples: Map<String, Example>): Result<Map<String, Any?>> =
    if (examples.isEmpty()) success(emptyMap())
    else examples.map { (key, example) ->
      sharedComponents.resolve(example).map { key to it.value?.normalize() }
    }.combineResults().map { it.toMap() }

  companion object {
    private val IGNORED_REQUEST_HEADERS = setOf("Accept", "Content-Type", "Authorization")
  }
}

private data class StyleDefaults(
  val defaultStyle: String,
  val defaultExplode: Boolean,
  val supportedStyles: Set<String>,
  val locationName: String
)
