package tech.sabai.contracteer.core.swagger

import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.headers.Header
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.combineResults
import tech.sabai.contracteer.core.operation.ContentType
import tech.sabai.contracteer.core.datatype.ArrayDataType
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.operation.BodySchema
import tech.sabai.contracteer.core.operation.ParameterElement
import tech.sabai.contracteer.core.operation.ParameterElement.*
import tech.sabai.contracteer.core.operation.ParameterSchema
import tech.sabai.contracteer.core.operation.RequestSchema
import tech.sabai.contracteer.core.operation.ResponseSchema
import tech.sabai.contracteer.core.serde.BasicSerde
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

  private fun extractPathParameterSchemas(operation: Operation): Result<List<ParameterSchema>> =
    operation.safeParameters()
      .filter { it.`in` == "path" }
      .map { if (it.safeIsRequired()) success(it) else failure("Path parameter ${it.name} is required") }
      .combineResults()
      .flatMap { parameters -> parameters!!.map { it.toParameterSchema(PathParam(it.name)) }.combineResults() }

  private fun extractQueryParameterSchemas(operation: Operation): Result<List<ParameterSchema>> =
    operation.safeParameters()
      .filter { it.`in` == "query" }
      .map { it.toParameterSchema(QueryParam(it.name)) }
      .combineResults()

  private fun extractRequestHeaderSchemas(operation: Operation): Result<List<ParameterSchema>> =
    operation.safeParameters()
      .filter { it.`in` == "header" }
      .map { it.toParameterSchema(ParameterElement.Header(it.name)) }
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
        .map { (contentType, mediaType) ->
          dataTypeConverter
            .convertToDataType(mediaType.schema, "")
            .flatMap { dataType ->
              validateBodySchemaContentType(ContentType(contentType), dataType!!)
                .map { BodySchema(ContentType(contentType), dataType, body.safeRequired()) }
            }
        }.combineResults()
        .forProperty("body")

  private fun extractResponseHeaderSchemas(response: ApiResponse): Result<List<ParameterSchema>> =
    response.safeHeaders().map { (name, header) -> header.toParameterSchema(name) }.combineResults().forProperty("header")

  private fun extractResponseBodySchemas(response: ApiResponse): Result<List<BodySchema>> =
    if (response.content == null)
      success(emptyList())
    else
      response.content
        .map { (contentType, mediaType) ->
          dataTypeConverter
            .convertToDataType(mediaType.schema, "")
            .flatMap { dataType ->
              validateBodySchemaContentType(ContentType(contentType), dataType!!)
                .map { BodySchema(ContentType(contentType), dataType, mediaType.schema.safeNullable()) }
            }
        }.combineResults()
        .forProperty("body")

  private fun Parameter.toParameterSchema(element: ParameterElement): Result<ParameterSchema> =
    sharedComponents.resolve(this).flatMap { resolved ->
      dataTypeConverter
        .convertToDataType(resolved!!.schema, "")
        .map { ParameterSchema(element, it!!, resolved.safeIsRequired(), BasicSerde) }
    }

  private fun Header.toParameterSchema(name: String): Result<ParameterSchema> =
    sharedComponents.resolve(this).flatMap { resolved ->
      dataTypeConverter
        .convertToDataType(resolved!!.schema, "")
        .map { ParameterSchema(ParameterElement.Header(name), it!!, resolved.safeIsRequired(), BasicSerde) }
    }

  private fun validateBodySchemaContentType(contentType: ContentType,
                                            dataType: DataType<out Any>): Result<DataType<out Any>> =
    if (contentType.isJson() && !dataType.isFullyStructured() && dataType !is ArrayDataType)
      failure("Content type ${contentType.value} supports only 'object', 'anyOf', 'oneOf', 'allOf' or 'array' schema")
    else
      success(dataType)
}
