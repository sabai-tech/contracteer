package tech.sabai.contracteer.core.swagger

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.responses.ApiResponse
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.combineResults
import tech.sabai.contracteer.core.normalize
import tech.sabai.contracteer.core.operation.*
import tech.sabai.contracteer.core.operation.ParameterElement.*

internal class ScenarioExtractor(private val sharedComponents: SharedComponents) {

  private val logger = KotlinLogging.logger {}

  fun extractScenarios(
    path: String,
    method: String,
    operation: Operation,
    requestSchema: Result<RequestSchema>,
    responseSchemas: Result<Map<Int, ResponseSchema>>
  ): Result<List<Scenario>> {
    if (requestSchema.isFailure()) return requestSchema.retypeError()
    if (responseSchemas.isFailure()) return responseSchemas.retypeError()
    val requestExampleKeys = operation.requestExampleKeys()
    if (requestExampleKeys.isEmpty()) return success(emptyList())

    val responseStatusCodes = operation.responses.keys.mapNotNull { it.toIntOrNull() }.toSet()
    requestExampleKeys.forEach { key ->
      val statusCode = key.statusCodePrefix()
      if (statusCode != null && statusCode !in responseStatusCodes) {
        logger.warn {
          "Operation '$method $path': example key '$key' targets status code $statusCode, " +
          "but no response with that status code is defined. Key ignored."
        }
      }
    }

    return operation.responses
      .map { (code, response) ->
        extractScenariosForResponse(
          path,
          method,
          code,
          response,
          operation,
          requestExampleKeys,
          requestSchema.value!!,
          responseSchemas.value!!)
      }
      .combineResults()
      .map { scenarios -> scenarios?.flatten() ?: emptyList() }
  }

  private fun extractScenariosForResponse(
    path: String,
    method: String,
    statusCode: String,
    response: ApiResponse,
    operation: Operation,
    requestExampleKeys: Set<String>,
    requestSchema: RequestSchema,
    responseSchemas: Map<Int, ResponseSchema>
  ): Result<List<Scenario>> {
    val parsedStatus = parseStatusCode(statusCode)
    if (parsedStatus.isFailure()) return parsedStatus.retypeError()

    val resolvedResponse = sharedComponents.resolve(response)
    if (resolvedResponse.isFailure()) return resolvedResponse.retypeError()

    val resolved = resolvedResponse.value!!
    val responseExampleKeys = resolved.exampleKeys()
    val statusCodePrefixedKeys = requestExampleKeys.filter { it.statusCodePrefix() == parsedStatus.value }
    val nonPrefixedKeys = requestExampleKeys.filter { it.statusCodePrefix() == null }
    val scenarioKeys = (nonPrefixedKeys intersect responseExampleKeys) union statusCodePrefixedKeys
    if (scenarioKeys.isEmpty()) return success(emptyList())

    val responseSchema = responseSchemas[parsedStatus.value!!]
                         ?: return failure("Response status code '${parsedStatus.value}' is not defined.")

    return scenarioKeys
      .map { key ->
        resolved.extractScenarioForKey(path, method, parsedStatus.value, operation, key, requestSchema, responseSchema)
      }
      .combineResults()
      .map { scenarios -> scenarios?.flatten() ?: emptyList() }
  }

  private fun ApiResponse.extractScenarioForKey(
    path: String,
    method: String,
    statusCode: Int,
    operation: Operation,
    key: String,
    requestSchema: RequestSchema,
    responseSchema: ResponseSchema
  ): Result<List<Scenario>> {
    val exampleValues = extractExampleValues(operation, key)
    if (exampleValues.isFailure()) return exampleValues.retypeError()

    val scenarios = generateScenarioCombinations(path, method, statusCode, key, exampleValues.value!!)

    return if (statusCode == 400)
      success(scenarios)
    else
      validateScenarios(key, exampleValues.value, requestSchema, responseSchema)
        .map { scenarios }
  }

  private fun ApiResponse.extractExampleValues(
    operation: Operation,
    key: String
  ): Result<ExampleValues> {
    val requestParams = operation.extractExampleParameterValues(key).forProperty("request")
    val requestBodies = operation.extractExampleRequestBodies(key).forProperty("requestBody")
    val responseHeaders = extractExampleHeaderValues(key).forProperty("response")
    val responseBodies = extractExampleResponseBodies(key).forProperty("responseBody")

    return if (allAreSuccess(requestParams, requestBodies, responseHeaders, responseBodies)) {
      success(ExampleValues(
        requestParams = requestParams.value!!,
        requestBodies = requestBodies.value!!.ifEmpty { listOf(null) },
        responseHeaders = responseHeaders.value!!,
        responseBodies = responseBodies.value!!.ifEmpty { listOf(null) }
      ))
    } else {
      requestParams.retypeError<ExampleValues>() combineWith
          requestBodies.retypeError() combineWith
          responseHeaders.retypeError() combineWith
          responseBodies.retypeError()
    }
  }

  private fun Operation.extractExampleParameterValues(exampleKey: String): Result<Map<ParameterElement, Any?>> =
    safeParameters()
      .filter { it.safeExamples().containsKey(exampleKey) }
      .map { parameter ->
        sharedComponents.resolve(parameter.safeExamples()[exampleKey]!!).map { it!!.value?.normalize() }
          .map { example ->
            when (parameter.`in`) {
              "path"   -> PathParam(parameter.name)
              "query"  -> QueryParam(parameter.name)
              "header" -> Header(parameter.name)
              "cookie" -> Cookie(parameter.name)
              else     -> Header(parameter.name)
            } to example
          }
      }
      .combineResults()
      .map { values -> values?.toMap() ?: emptyMap() }

  private fun Operation.extractExampleRequestBodies(exampleKey: String): Result<List<ScenarioBody>> =
    if (requestBody?.content == null)
      success(emptyList())
    else
      requestBody!!.content
        .filter { (_, mediaType) -> mediaType.safeExamples().containsKey(exampleKey) }
        .map { (contentType, mediaType) ->
          sharedComponents.resolve(mediaType.safeExamples()[exampleKey]!!)
            .map { it!!.value?.normalize() }
            .map { example -> ScenarioBody(ContentType(contentType), example) }
        }
        .combineResults()
        .map { bodies -> bodies ?: emptyList() }

  private fun ApiResponse.extractExampleHeaderValues(exampleKey: String): Result<Map<Header, Any?>> =
    safeHeaders()
      .filter { (_, header) -> header.safeExamples().containsKey(exampleKey) }
      .map { (name, header) ->
        sharedComponents.resolve(header.safeExamples()[exampleKey]!!)
          .map { it!!.value?.normalize() }
          .map { example -> Header(name) to example }
      }
      .combineResults()
      .map { values -> values?.toMap() ?: emptyMap() }

  private fun ApiResponse.extractExampleResponseBodies(exampleKey: String): Result<List<ScenarioBody>> =
    if (content == null)
      success(emptyList())
    else
      content
        .filter { (_, mediaType) -> mediaType.safeExamples().containsKey(exampleKey) }
        .map { (contentType, mediaType) ->
          sharedComponents.resolve(mediaType.safeExamples()[exampleKey]!!)
            .map { it!!.value?.normalize() }
            .map { example -> ScenarioBody(ContentType(contentType), example) }
        }
        .combineResults()
        .map { bodies -> bodies ?: emptyList() }

  private fun generateScenarioCombinations(
    path: String,
    method: String,
    statusCode: Int,
    key: String,
    examples: ExampleValues
  ): List<Scenario> =
    examples.requestBodies.flatMap { reqBody ->
      examples.responseBodies.map { resBody ->
        Scenario(
          path = path,
          method = method,
          key = key,
          statusCode = statusCode,
          request = ScenarioRequest(examples.requestParams, reqBody),
          response = ScenarioResponse(examples.responseHeaders, resBody)
        )
      }
    }

  private fun validateScenarios(
    key: String,
    examples: ExampleValues,
    requestSchema: RequestSchema,
    responseSchema: ResponseSchema
  ): Result<Any?> =
    validateScenarioExamples(
      key,
      ScenarioRequestExamples(examples.requestParams, examples.requestBodies, requestSchema),
      ScenarioResponseExamples(examples.responseHeaders, examples.responseBodies, responseSchema)
    )

  private fun validateScenarioExamples(
    exampleKey: String,
    request: ScenarioRequestExamples,
    response: ScenarioResponseExamples
  ): Result<Any?> {
    val requestValidation = validateRequestExamples(exampleKey, request)
    val responseValidation = validateResponseExamples(exampleKey, response)
    return requestValidation.combineWith(responseValidation).retypeError()
  }

  private fun validateRequestExamples(exampleKey: String, request: ScenarioRequestExamples): Result<Any?> {
    val schemasByElement = request.schema.parameters.associateBy { it.element }
    val paramValidation = request.values.map { (element, value) ->
      when (val schema = schemasByElement[element]) {
        null -> failure("Example '$exampleKey' has no schema for request parameter '${elementName(element)}'")
        else -> schema.dataType.validate(value).forProperty(elementName(element))
      }
    }.combineResults().retypeError<Any?>()

    val bodyValidation = request.bodies.map { body ->
      if (body == null)
        success()
      else {
        when (val schema = request.schema.bodies.find { it.contentType.value == body.contentType.value }) {
          null -> failure("Example '$exampleKey' has no schema for request body '${body.contentType.value}'")
          else -> schema.dataType.validate(body.value).forProperty("body")
        }
      }
    }.combineResults().retypeError<Any?>()

    return paramValidation.combineWith(bodyValidation).retypeError()
  }

  private fun validateResponseExamples(exampleKey: String, response: ScenarioResponseExamples): Result<Any?> {
    val schemasByElement = response.schema.headers.associateBy { it.element }
    val headerValidation = response.values.map { (element, value) ->
      when (val schema = schemasByElement[element]) {
        null -> failure("Example '$exampleKey' has no schema for response header '${elementName(element)}'")
        else -> schema.dataType.validate(value).forProperty(elementName(element))
      }
    }.combineResults().retypeError<Any?>()

    val bodyValidation = response.bodies.map { body ->
      if (body == null)
        success()
      else {
        when (val schema = response.schema.bodies.find { it.contentType.value == body.contentType.value }) {
          null -> failure("Example '$exampleKey' has no schema for response body '${body.contentType.value}'")
          else -> schema.dataType.validate(body.value).forProperty("body")
        }
      }
    }.combineResults().retypeError<Any?>()

    return headerValidation.combineWith(bodyValidation).retypeError()
  }

  private fun elementName(element: ParameterElement) =
    when (element) {
      is PathParam  -> "path.${element.name}"
      is QueryParam -> "query.${element.name}"
      is Header     -> "header.${element.name}"
      is Cookie     -> "cookie.${element.name}"
    }

  private data class ExampleValues(
    val requestParams: Map<ParameterElement, Any?>,
    val requestBodies: List<ScenarioBody?>,
    val responseHeaders: Map<Header, Any?>,
    val responseBodies: List<ScenarioBody?>
  )

  private data class ScenarioRequestExamples(
    val values: Map<ParameterElement, Any?>,
    val bodies: List<ScenarioBody?>,
    val schema: RequestSchema
  )

  private data class ScenarioResponseExamples(
    val values: Map<Header, Any?>,
    val bodies: List<ScenarioBody?>,
    val schema: ResponseSchema
  )
}

internal fun List<Parameter>.exampleKeys() =
  flatMap { it.safeExamples().keys }.toSet()

internal fun Content.exampleKeys() =
  flatMap { it.value.safeExamples().keys }.toSet()

internal fun ApiResponse.exampleKeys() =
  safeHeaders().exampleKeys() + bodyExampleKeys()

internal fun ApiResponse.bodyExampleKeys() =
  content?.exampleKeys() ?: emptySet()

internal fun Operation.requestExampleKeys() =
  safeParameters().exampleKeys() + (requestBody?.content?.exampleKeys() ?: emptySet())

internal fun Map<String, io.swagger.v3.oas.models.headers.Header>.exampleKeys() =
  flatMap { it.value.safeExamples().keys }.toSet()

private val STATUS_CODE_PREFIX = Regex("""^(\d{3})_\w+$""")
private fun String.statusCodePrefix(): Int? =
  STATUS_CODE_PREFIX.matchEntire(this)?.groupValues?.get(1)?.toIntOrNull()
