package tech.sabai.contracteer.core.swagger

import io.swagger.v3.oas.models.Operation
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
import tech.sabai.contracteer.core.operation.*
import tech.sabai.contracteer.core.operation.ParameterElement.*
import tech.sabai.contracteer.core.serde.*
import tech.sabai.contracteer.core.swagger.datatype.DataTypeConverter

internal class SchemaExtractor(
  private val sharedComponents: SharedComponents,
  private val dataTypeConverter: DataTypeConverter
) {

  fun extractRequestSchema(operation: Operation): Result<RequestSchema> {
    val pathParameters = extractPathParameterSchemas(operation).forProperty("request.path")
    val queryParameters = extractQueryParameterSchemas(operation).forProperty("request.query")
    val headers = extractRequestHeaderSchemas(operation).forProperty("request.header")
    val cookies = extractRequestCookieSchemas(operation).forProperty("request.cookie")
    val bodies = extractRequestBodySchemas(operation).forProperty("request.body")

    return if (pathParameters is Success && queryParameters is Success && headers is Success && cookies is Success && bodies is Success)
      success(RequestSchema(
        parameters = pathParameters.value + queryParameters.value + headers.value + cookies.value,
        bodies = bodies.value
      ))
    else
      (pathParameters combineWith
          queryParameters combineWith
          headers combineWith
          cookies combineWith bodies).retypeError()
  }

  fun extractResponseSchema(code: String, response: ApiResponse): Result<Pair<Int, ResponseSchema>> =
    parseStatusCode(code).flatMap { statusCode ->
      sharedComponents.resolve(response).flatMap { resolved ->
        val headers = extractResponseHeaderSchemas(resolved)
        val bodies = extractResponseBodySchemas(resolved)
        if (headers is Success && bodies is Success)
          success(statusCode to ResponseSchema(headers = headers.value, bodies = bodies.value))
        else
          (headers combineWith bodies).forKey(code).forProperty("response").retypeError()
      }
    }

  fun extractResponseSchema(response: ApiResponse): Result<ResponseSchema> =
    sharedComponents.resolve(response).flatMap { resolved ->
      val headers = extractResponseHeaderSchemas(resolved)
      val bodies = extractResponseBodySchemas(resolved)
      if (headers is Success && bodies is Success)
        success(ResponseSchema(headers = headers.value, bodies = bodies.value))
      else
        (headers combineWith bodies).retypeError()
    }

  private fun extractPathParameterSchemas(operation: Operation): Result<List<ParameterSchema>> =
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

  private fun extractQueryParameterSchemas(operation: Operation): Result<List<ParameterSchema>> =
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

  private fun extractRequestHeaderSchemas(operation: Operation): Result<List<ParameterSchema>> =
    operation.safeParameters()
      .filter { it.`in` == "header" && IGNORED_REQUEST_HEADERS.none { h -> h.equals(it.name, ignoreCase = true) } }
      .map { it.toParameterSchema(Header(it.name)) }
      .combineResults()

  private fun extractRequestCookieSchemas(operation: Operation): Result<List<ParameterSchema>> =
    operation.safeParameters()
      .filter { it.`in` == "cookie" }
      .map { it.toParameterSchema(Cookie(it.name)) }
      .combineResults()

  private fun extractRequestBodySchemas(operation: Operation): Result<List<BodySchema>> =
    if (operation.requestBody == null)
      success(emptyList())
    else
      extractRequestBodySchemas(operation.requestBody!!)

  private fun extractRequestBodySchemas(requestBody: RequestBody): Result<List<BodySchema>> =
    sharedComponents.resolve(requestBody).flatMap { convertRequestBodySchema(it) }

  private fun convertRequestBodySchema(body: RequestBody): Result<List<BodySchema>> =
    if (body.content == null)
      success(emptyList())
    else
      body.content
        .map { ContentType(it.key) to it.value!! }
        .map { (contentType, mediaType) ->
          convertDataType(mediaType).flatMap { dataType ->
            if (dataType is AnyDataType)
              success(BodySchema(contentType, dataType, body.safeRequired(), PlainTextSerde))
            else
              buildSerde(contentType, mediaType, dataType)
                .map { BodySchema(contentType, dataType.asRequestType(), body.safeRequired(), it) }
          }
        }.combineResults()

  private fun extractResponseHeaderSchemas(response: ApiResponse): Result<List<ParameterSchema>> =
    response
      .safeHeaders()
      .filterKeys { !it.equals("Content-Type", ignoreCase = true) }
      .map { (name, header) -> header.toParameterSchema(name) }
      .combineResults()
      .forProperty("header")

  private fun extractResponseBodySchemas(response: ApiResponse): Result<List<BodySchema>> =
    if (response.content == null)
      success(emptyList())
    else
      response.content
        .map { ContentType(it.key) to it.value!! }
        .map { (contentType, mediaType) ->
          convertDataType(mediaType).flatMap { dataType ->
            if (dataType is AnyDataType)
              success(BodySchema(contentType, dataType, false, PlainTextSerde))
            else
              buildSerde(contentType, mediaType, dataType)
                .map {
                  BodySchema(contentType, dataType.asResponseType(), mediaType.schema.safeNullable(), it)
                }
          }
        }.combineResults()
        .forProperty("body")

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

  private fun Parameter.toParameterSchema(element: ParameterElement): Result<ParameterSchema> =
    sharedComponents.resolve(this).flatMap { resolved ->
      if (resolved.content != null && resolved.content.isNotEmpty())
        createContentParameterSchema(resolved, element)
      else
        createStyleParameterSchema(resolved, element)
    }

  private fun createContentParameterSchema(parameter: Parameter, element: ParameterElement): Result<ParameterSchema> {
    val (mediaTypeString, mediaTypeObj) = parameter.content.entries.first()
    val contentType = ContentType(mediaTypeString)
    return convertDataType(mediaTypeObj).flatMap { dataType ->
      if (dataType is AnyDataType)
        success(ParameterSchema(element,
                                dataType,
                                parameter.safeIsRequired(),
                                ContentCodec(parameter.name, PlainTextSerde))
        )
      else
        buildSerde(contentType, mediaTypeObj, dataType)
          .map { ParameterSchema(element, dataType, parameter.safeIsRequired(), ContentCodec(parameter.name, it)) }
    }
  }

  private fun createStyleParameterSchema(parameter: Parameter, element: ParameterElement): Result<ParameterSchema> =
    dataTypeConverter
      .convertToDataType(parameter.schema, "")
      .flatMap { dataType ->
        createCodecForParameter(element, parameter.style?.toString(), parameter.explode, dataType, parameter.name)
          .map { ParameterSchema(element, dataType, parameter.safeIsRequired(), it) }
      }

  private fun Header.toParameterSchema(name: String): Result<ParameterSchema> =
    sharedComponents.resolve(this).flatMap { resolved ->
      dataTypeConverter
        .convertToDataType(resolved.schema, "")
        .flatMap { dataType ->
          createCodecForParameter(Header(name), resolved.style?.toString(), resolved.explode, dataType, name)
            .map { ParameterSchema(Header(name), dataType, resolved.safeIsRequired(), it) }
        }
    }

  private fun createCodecForParameter(element: ParameterElement,
                                      style: String?,
                                      explode: Boolean?,
                                      dataType: DataType<out Any>,
                                      paramName: String): Result<ParameterCodec> {
    val normalizedStyle = style?.lowercase()?.replace("_", "")
    val (defaultStyle, defaultExplode) = when (element) {
      is PathParam               -> "simple" to false
      is QueryParam              -> "form" to true
      is ParameterElement.Header -> "simple" to false
      is Cookie                  -> "form" to true
    }
    val actualStyle = normalizedStyle ?: defaultStyle
    val actualExplode = explode ?: defaultExplode
    val supportedStyles = when (element) {
      is PathParam               -> setOf("simple", "label", "matrix")
      is QueryParam              -> setOf("form", "spacedelimited", "pipedelimited", "deepobject")
      is ParameterElement.Header -> setOf("simple")
      is Cookie                  -> setOf("form")
    }

    if (actualStyle !in supportedStyles) {
      val locationName = when (element) {
        is PathParam               -> "path"
        is QueryParam              -> "query"
        is ParameterElement.Header -> "header"
        is Cookie                  -> "cookie"
      }
      return failureForKey(paramName, "Style '${style ?: actualStyle}' is not supported for $locationName parameters")
    }

    validateStyleConstraints(actualStyle, actualExplode, dataType, paramName)?.let { return it }

    return when (actualStyle) {
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

  private fun validateStyleConstraints(style: String,
                                       explode: Boolean,
                                       dataType: DataType<out Any>,
                                       paramName: String): Result<ParameterCodec>? =
    when (style) {
      "deepobject"     -> validateDeepObjectParameters(dataType, paramName, explode)
      "spacedelimited" -> validateSpaceDelimitedParameters(dataType, paramName, explode)
      "pipedelimited"  -> validatePipeDelimitedParameters(dataType, paramName, explode)
      else             -> null
    }

  private fun validateDeepObjectParameters(dataType: DataType<out Any>,
                                           paramName: String,
                                           explode: Boolean): Result<ParameterCodec>? =
    when {
      dataType !is ObjectDataType          -> failureForKey(paramName, "Style 'deepObject' requires object type")
      !explode                             -> failureForKey(paramName, "Style 'deepObject' requires explode=true")
      dataType.hasNonPrimitiveProperties() -> failureForKey(paramName, "Style 'deepObject' does not support nested objects or arrays in properties (undefined behavior in the OpenAPI specification)")
      else                                 -> null
    }

  private fun validatePipeDelimitedParameters(dataType: DataType<out Any>,
                                              paramName: String,
                                              explode: Boolean): Result<ParameterCodec>? = when {
    dataType !is ArrayDataType -> failureForKey(paramName, "Style 'pipeDelimited' requires array type")
    explode                    -> failureForKey(paramName, "Style 'pipeDelimited' requires explode=false")
    else                       -> null
  }

  private fun validateSpaceDelimitedParameters(dataType: DataType<out Any>,
                                               paramName: String,
                                               explode: Boolean): Result<ParameterCodec>? = when {
    dataType !is ArrayDataType -> failureForKey(paramName, "Style 'spaceDelimited' requires array type")
    explode                    -> failureForKey(paramName, "Style 'spaceDelimited' requires explode=false")
    else                       -> null
  }

  private fun ObjectDataType.hasNonPrimitiveProperties(): Boolean =
    properties.values.any { it.isNonPrimitive() }

  private fun DataType<out Any>.isNonPrimitive(): Boolean =
    isFullyStructured() || this is ArrayDataType

  private fun DataType<out Any>.isBinary() = this is BinaryDataType || this is Base64DataType

  private fun parseStatusCode(code: String): Result<Int> =
    code.toIntOrNull()?.let { success(it) } ?: failure("Response status code '$code' is not supported.")

  companion object {
    private val IGNORED_REQUEST_HEADERS = setOf("Accept", "Content-Type", "Authorization")
  }
}
