package tech.sabai.contracteer.core.swagger

import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.responses.ApiResponse
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.Result.Success
import tech.sabai.contracteer.core.combineResults
import tech.sabai.contracteer.core.result
import tech.sabai.contracteer.core.swagger.datatype.DataTypeConverter

internal class SchemaExtractor(
  private val sharedComponents: SharedComponents,
  dataTypeConverter: DataTypeConverter
) {

  private val codecFactory = CodecFactory()
  private val serdeFactory = SerdeFactory(codecFactory)
  private val parameterExtractor = ParameterExtractor(sharedComponents, dataTypeConverter, codecFactory, serdeFactory)
  private val bodyExtractor = BodyExtractor(sharedComponents, dataTypeConverter, serdeFactory)

  fun extractRequestSchema(operation: Operation): Result<ExtractedRequestSchema> {
    val parameters = listOf(
      parameterExtractor.extractPath(operation).forProperty("request.path"),
      parameterExtractor.extractQuery(operation).forProperty("request.query"),
      parameterExtractor.extractRequestHeaders(operation).forProperty("request.header"),
      parameterExtractor.extractCookies(operation).forProperty("request.cookie")
    ).combineResults().map { it.flatten() }
    val bodies = bodyExtractor.extractRequestBodies(operation).forProperty("request.body")

    return when (parameters) {
      is Success if bodies is Success -> success(ExtractedRequestSchema(parameters.value, bodies.value))
      else                            -> (parameters combineWith bodies).retypeError()
    }
  }

  fun extractResponseSchema(code: String, response: ApiResponse): Result<Pair<Int, ExtractedResponseSchema>> =
    result {
      val statusCode = parseStatusCode(code).bind()
      val resolved = sharedComponents.resolve(response).bind()
      rejectBodylessResponseWithBody(statusCode, resolved).bind()
      val schema = buildResponseSchema(resolved).forKey(code).forProperty("response").bind()
      statusCode to schema
    }

  fun extractResponseSchema(response: ApiResponse): Result<ExtractedResponseSchema> =
    sharedComponents.resolve(response).flatMap { buildResponseSchema(it) }

  private fun rejectBodylessResponseWithBody(statusCode: Int, response: ApiResponse): Result<Unit> =
    if (isBodylessStatusCode(statusCode) && !response.content.isNullOrEmpty())
      failure("Response $statusCode declares a body, but HTTP $statusCode MUST NOT include a message body (RFC 7231)")
    else
      success()

  private fun buildResponseSchema(response: ApiResponse): Result<ExtractedResponseSchema> {
    val headers = parameterExtractor.extractResponseHeaders(response).forProperty("header")
    val bodies = bodyExtractor.extractResponseBodies(response).forProperty("body")
    return if (headers is Success && bodies is Success)
      success(ExtractedResponseSchema(headers.value, bodies.value))
    else
      (headers combineWith bodies).retypeError()
  }

  private fun parseStatusCode(code: String): Result<Int> =
    code.toIntOrNull()?.let { success(it) } ?: failure("Response status code '$code' is not supported")
}
