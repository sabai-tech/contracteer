package tech.sabai.contracteer.core.swagger

import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.headers.Header
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
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
    val pathParameters = extractPathParameterSchemas(operation).forProperty("path")
    val queryParameters = extractQueryParameterSchemas(operation).forProperty("query")
    val headers = extractRequestHeaderSchemas(operation).forProperty("header")
    val cookies = extractRequestCookieSchemas(operation).forProperty("cookie")
    val bodies = extractRequestBodySchemas(operation).forProperty("body")

    return if (allAreSuccess(pathParameters, queryParameters, headers, cookies, bodies))
      success(RequestSchema(
        parameters = pathParameters.value!! + queryParameters.value!! + headers.value!! + cookies.value!!,
        bodies = bodies.value!!
      ))
    else
      pathParameters.retypeError<RequestSchema>() combineWith
          queryParameters.retypeError() combineWith
          headers.retypeError() combineWith
          cookies.retypeError() combineWith
          bodies.retypeError()
  }

  fun extractResponseSchema(code: String, response: ApiResponse): Result<Pair<Int, ResponseSchema>> =
    parseStatusCode(code).flatMap { statusCode ->
      sharedComponents.resolve(response).flatMap { resolved ->
        val headers = extractResponseHeaderSchemas(resolved!!)
        val bodies = extractResponseBodySchemas(resolved)
        if (allAreSuccess(headers, bodies))
          success(statusCode!! to ResponseSchema(headers = headers.value!!, bodies = bodies.value!!))
        else
          headers.retypeError<Pair<Int, ResponseSchema>>() combineWith bodies.retypeError()
      }
    }

  fun extractResponseSchema(response: ApiResponse): Result<ResponseSchema> =
    sharedComponents.resolve(response).flatMap { resolved ->
      val headers = extractResponseHeaderSchemas(resolved!!)
      val bodies = extractResponseBodySchemas(resolved)
      if (allAreSuccess(headers, bodies))
        success(ResponseSchema(headers = headers.value!!, bodies = bodies.value!!))
      else
        headers.retypeError<ResponseSchema>() combineWith bodies.retypeError()
    }

  private fun extractPathParameterSchemas(operation: Operation): Result<List<ParameterSchema>> =
    operation.safeParameters()
      .filter { it.`in` == "path" }
      .map { if (it.safeIsRequired()) success(it) else failure("Path parameter ${it.name} is required") }
      .combineResults()
      .flatMap { parameters -> parameters!!.map { it.toParameterSchema(PathParam(it.name)) }.combineResults() }

  private fun extractQueryParameterSchemas(operation: Operation): Result<List<ParameterSchema>> =
    operation.safeParameters()
      .filter { it.`in` == "query" }
      .map { param ->
        sharedComponents
          .resolve(param)
          .flatMap { resolved ->
            resolved!!.toParameterSchema(QueryParam(resolved.name, resolved.safeAllowReserved()))
          }
      }
      .combineResults()

  private fun extractRequestHeaderSchemas(operation: Operation): Result<List<ParameterSchema>> =
    operation.safeParameters()
      .filter { it.`in` == "header" }
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
    sharedComponents.resolve(requestBody).flatMap { convertRequestBodySchema(it!!) }

  private fun convertRequestBodySchema(body: RequestBody): Result<List<BodySchema>> =
    if (body.content == null)
      success(emptyList())
    else
      body.content
        .map { ContentType(it.key) to it.value!! }
        .map { (contentType, mediaType) ->
          dataTypeConverter
            .convertToDataType(mediaType.schema, "")
            .flatMap { dataType ->
              buildSerde(contentType, mediaType, dataType!!)
                .map {
                  BodySchema(contentType, dataType.asRequestType(), body.safeRequired(), it!!)
                }
            }
        }.combineResults()

  private fun extractResponseHeaderSchemas(response: ApiResponse): Result<List<ParameterSchema>> =
    response
      .safeHeaders()
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
          dataTypeConverter
            .convertToDataType(mediaType.schema, "")
            .flatMap { dataType ->
              buildSerde(contentType, mediaType, dataType!!)
                .map {
                  BodySchema(contentType, dataType.asResponseType(), mediaType.schema.safeNullable(), it!!)
                }
            }
        }.combineResults()
        .forProperty("body")

  private fun buildSerde(contentType: ContentType,
                         mediaType: MediaType,
                         dataType: DataType<out Any>): Result<Serde> =
    when {
      contentType.isJson() && !dataType.isFullyStructured() && dataType !is ArrayDataType ->
        failure("Content type ${contentType.value} supports only 'object', 'anyOf', 'oneOf', 'allOf' or 'array' schema")

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

      else                                                                                ->
        success(PlainTextSerde)
    }

  private fun buildFormUrlEncodedSerde(dataType: ObjectDataType, mediaType: MediaType): Result<Serde> {
    val encodingMap = mediaType.encoding ?: emptyMap()
    return dataType.properties
      .map { (name, type) ->
        val encoding = encodingMap[name]
        val allowReserved = encoding?.allowReserved == true
        createCodecForParameter(QueryParam(name), encoding?.style?.toString(), encoding?.explode, type, name)
          .map { name to FormUrlEncodedSerde.PropertyEncoding(it!!, allowReserved) }
      }
      .combineResults()
      .map<Serde> { FormUrlEncodedSerde(it!!.toMap()) }
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
      dataTypeConverter
        .convertToDataType(resolved!!.schema, "")
        .flatMap { dataType ->
          createCodecForParameter(element, resolved.style?.toString(), resolved.explode, dataType!!, name)
            .map { ParameterSchema(element, dataType, resolved.safeIsRequired(), it!!) }
        }
    }

  private fun Header.toParameterSchema(name: String): Result<ParameterSchema> =
    sharedComponents.resolve(this).flatMap { resolved ->
      dataTypeConverter
        .convertToDataType(resolved!!.schema, "")
        .flatMap { dataType ->
          createCodecForParameter(Header(name), resolved.style?.toString(), resolved.explode, dataType!!, name)
            .map { ParameterSchema(Header(name), dataType, resolved.safeIsRequired(), it!!) }
        }
    }

  private fun createCodecForParameter(element: ParameterElement,
                                      style: String?,
                                      explode: Boolean?,
                                      dataType: DataType<out Any>,
                                      paramName: String): Result<StyleCodec> {
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
      return failure(paramName, "Style '${style ?: actualStyle}' is not supported for $locationName parameters")
    }

    validateStyleConstraints(actualStyle, actualExplode, dataType, paramName)?.let { return it }

    return success(when (actualStyle) {
                     "simple"         -> SimpleStyleCodec(paramName, actualExplode)
                     "form"           -> FormStyleCodec(paramName, actualExplode)
                     "label"          -> LabelStyleCodec(paramName, actualExplode)
                     "matrix"         -> MatrixStyleCodec(paramName, actualExplode)
                     "spacedelimited" -> SpaceDelimitedStyleCodec(paramName)
                     "pipedelimited"  -> PipeDelimitedStyleCodec(paramName)
                     "deepobject"     -> DeepObjectStyleCodec(paramName)
                     else             -> return failure(paramName, "Unknown style '$actualStyle'")
                   })
  }

  private fun validateStyleConstraints(
    style: String,
    explode: Boolean,
    dataType: DataType<out Any>,
    paramName: String
  ): Result<StyleCodec>? = when (style) {
    "deepobject"     -> when {
      dataType !is ObjectDataType -> failure(paramName, "Style 'deepObject' requires object type")
      !explode                    -> failure(paramName, "Style 'deepObject' requires explode=true")
      else                        -> null
    }
    "spacedelimited" -> when {
      dataType !is ArrayDataType -> failure(paramName, "Style 'spaceDelimited' requires array type")
      explode                    -> failure(paramName, "Style 'spaceDelimited' requires explode=false")
      else                       -> null
    }
    "pipedelimited"  -> when {
      dataType !is ArrayDataType -> failure(paramName, "Style 'pipeDelimited' requires array type")
      explode                    -> failure(paramName, "Style 'pipeDelimited' requires explode=false")
      else                       -> null
    }
    else             -> null
  }

  private fun DataType<out Any>.isBinary() = this is BinaryDataType || this is Base64DataType
}
