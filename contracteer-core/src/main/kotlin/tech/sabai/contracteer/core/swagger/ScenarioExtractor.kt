package tech.sabai.contracteer.core.swagger

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.responses.ApiResponse
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.Result.Success
import tech.sabai.contracteer.core.combineResults
import tech.sabai.contracteer.core.normalize
import tech.sabai.contracteer.core.operation.*
import tech.sabai.contracteer.core.operation.ParameterElement.*

internal class ScenarioExtractor(private val sharedComponents: SharedComponents) {

  private val logger = KotlinLogging.logger {}

  fun extractScenarios(path: String,
                       method: String,
                       operation: Operation,
                       requestSchema: RequestSchema,
                       responseSchemas: Map<Int, ResponseSchema>,
                       classResponses: Map<Int, ResponseSchema>,
                       defaultResponse: ResponseSchema?): Result<List<Scenario>> {
    val requestExampleKeys = operation.requestExampleKeys()
    if (requestExampleKeys.isEmpty()) return success(emptyList())

    val explicitStatusCodes = operation.responses.keys.mapNotNull { it.toIntOrNull() }.toSet()
    warnUnresolvablePrefixedKeys(method, path, requestExampleKeys, explicitStatusCodes, classResponses, defaultResponse)

    val explicitScenarios = operation.responses
      .filter { (code, _) -> code.toIntOrNull() != null }
      .map { (code, response) ->
        extractScenariosForResponse(path,method,code.toInt(),response,operation,requestExampleKeys,requestSchema,responseSchemas)
      }
      .combineResults()
      .map { scenarios -> scenarios.flatten() }

    val fallbackScenarios = extractScenariosFromFallbackResponses(path,
                                                                  method,
                                                                  operation,
                                                                  requestExampleKeys,
                                                                  explicitStatusCodes,
                                                                  requestSchema,
                                                                  classResponses,
                                                                  defaultResponse)

    return explicitScenarios.flatMap { explicit ->
      fallbackScenarios.map { fallback -> explicit + fallback }
    }
  }

  private fun warnUnresolvablePrefixedKeys(method: String,
                                           path: String,
                                           requestExampleKeys: Set<String>,
                                           explicitStatusCodes: Set<Int>,
                                           classResponses: Map<Int, ResponseSchema>,
                                           defaultResponse: ResponseSchema?) {
    requestExampleKeys.forEach { key ->
      val statusCode = key.statusCodePrefix()
      if (statusCode != null
          && statusCode !in explicitStatusCodes
          && classResponses[statusCode / 100] == null
          && defaultResponse == null) {
        logger.warn {
          "Operation '$method $path': example key '$key' targets status code $statusCode, " +
          "but no response with that status code is defined. Key ignored."
        }
      }
    }
  }

  private fun extractScenariosForResponse(path: String,
                                          method: String,
                                          statusCode: Int,
                                          response: ApiResponse,
                                          operation: Operation,
                                          requestExampleKeys: Set<String>,
                                          requestSchema: RequestSchema,
                                          responseSchemas: Map<Int, ResponseSchema>): Result<List<Scenario>> =
    sharedComponents
      .resolve(response)
      .flatMap { resolved ->
        extractScenariosFromResolved(path,method,statusCode,resolved,operation,requestExampleKeys,requestSchema,responseSchemas)
      }

  private fun extractScenariosFromResolved(path: String,
                                           method: String,
                                           statusCode: Int,
                                           resolved: ApiResponse,
                                           operation: Operation,
                                           requestExampleKeys: Set<String>,
                                           requestSchema: RequestSchema,
                                           responseSchemas: Map<Int, ResponseSchema>): Result<List<Scenario>> {
    val responseExampleKeys = resolved.exampleKeys()
    val statusCodePrefixedKeys = requestExampleKeys.filter { it.statusCodePrefix() == statusCode }
    val nonPrefixedKeys = requestExampleKeys.filter { it.statusCodePrefix() == null }
    val scenarioKeys = (nonPrefixedKeys intersect responseExampleKeys) union statusCodePrefixedKeys

    if (scenarioKeys.isEmpty()) return success(emptyList())

    val responseSchema = responseSchemas[statusCode]
                         ?: return failure("Response status code '$statusCode' is not defined.")

    return scenarioKeys
      .map { key ->
        resolved.extractScenarioForKey(path, method, statusCode, operation, key, requestSchema, responseSchema)
      }
      .combineResults()
      .map { scenarios -> scenarios.flatten() }
  }

  private fun extractScenariosFromFallbackResponses(path: String,
                                                    method: String,
                                                    operation: Operation,
                                                    requestExampleKeys: Set<String>,
                                                    explicitStatusCodes: Set<Int>,
                                                    requestSchema: RequestSchema,
                                                    classResponses: Map<Int, ResponseSchema>,
                                                    defaultResponse: ResponseSchema?): Result<List<Scenario>> {
    val unmatchedPrefixedKeys = requestExampleKeys.filter { key ->
      val prefix = key.statusCodePrefix()
      prefix != null && prefix !in explicitStatusCodes
    }

    return when {
      unmatchedPrefixedKeys.isEmpty() -> success(emptyList())
      else                            ->
        unmatchedPrefixedKeys
          .map { key ->
            extractScenarioForFallbackKey(path, method, operation, key, requestSchema, classResponses, defaultResponse)
          }
          .combineResults()
          .map { scenarios -> scenarios.flatten() }
    }
  }

  private fun extractScenarioForFallbackKey(path: String,
                                            method: String,
                                            operation: Operation,
                                            key: String,
                                            requestSchema: RequestSchema,
                                            classResponses: Map<Int, ResponseSchema>,
                                            defaultResponse: ResponseSchema?): Result<List<Scenario>> {
    val statusCode = key.statusCodePrefix()!!
    val (apiResponse, responseSchema) = findFallbackResponse(operation, statusCode, classResponses, defaultResponse)
                                        ?: return success(emptyList())

    return sharedComponents.resolve(apiResponse).flatMap { resolved ->
      resolved.extractScenarioForKey(path, method, statusCode, operation, key, requestSchema, responseSchema)
    }
  }

  private fun findFallbackResponse(operation: Operation,
                                   statusCode: Int,
                                   classResponses: Map<Int, ResponseSchema>,
                                   defaultResponse: ResponseSchema?): Pair<ApiResponse, ResponseSchema>? =
    findClassResponse(operation, statusCode, classResponses)
    ?: findDefaultResponse(operation, defaultResponse)

  private fun findClassResponse(operation: Operation,
                                statusCode: Int,
                                classResponses: Map<Int, ResponseSchema>): Pair<ApiResponse, ResponseSchema>? {
    val classDigit = statusCode / 100
    val schema = classResponses[classDigit] ?: return null
    val apiResponse = operation.responses.entries
                        .firstOrNull { (code, _) -> isClassCode(code) && code[0].digitToInt() == classDigit }
                        ?.value ?: return null
    return apiResponse to schema
  }

  private fun findDefaultResponse(operation: Operation,
                                  defaultResponse: ResponseSchema?): Pair<ApiResponse, ResponseSchema>? {
    val schema = defaultResponse ?: return null
    val apiResponse = operation.responses["default"] ?: return null
    return apiResponse to schema
  }

  // --- Shared: example extraction, scenario generation, validation ---

  private fun ApiResponse.extractScenarioForKey(path: String,
                                                method: String,
                                                statusCode: Int,
                                                operation: Operation,
                                                key: String,
                                                requestSchema: RequestSchema,
                                                responseSchema: ResponseSchema): Result<List<Scenario>> {
    return extractExampleValues(operation, key)
      .flatMap { examples ->
        val scenarios = generateScenarioCombinations(path, method, statusCode, key, examples)
        if (statusCode == 400)
          success(scenarios)
        else
          validateScenarios(key, examples, requestSchema, responseSchema)
            .map { scenarios }
      }
  }

  private fun ApiResponse.extractExampleValues(operation: Operation, key: String): Result<ExampleValues> {
    val requestParams = operation.extractExampleParameterValues(key).forProperty("request")
    val requestBodies = operation.extractExampleRequestBodies(key).forProperty("requestBody")
    val responseHeaders = extractExampleHeaderValues(key).forProperty("response")
    val responseBodies = extractExampleResponseBodies(key).forProperty("responseBody")

    return if (requestParams is Success && requestBodies is Success && responseHeaders is Success && responseBodies is Success) {
      success(ExampleValues(
        requestParams = requestParams.value,
        requestBodies = requestBodies.value.ifEmpty { listOf(null) },
        responseHeaders = responseHeaders.value,
        responseBodies = responseBodies.value.ifEmpty { listOf(null) }
      ))
    } else {
      (requestParams combineWith requestBodies combineWith responseHeaders combineWith responseBodies).retypeError()
    }
  }

  private fun Operation.extractExampleParameterValues(exampleKey: String): Result<Map<ParameterElement, Any?>> =
    safeParameters()
      .filter { it.safeExamples().containsKey(exampleKey) }
      .map { it.resolveExampleEntry(exampleKey) }
      .combineResults()
      .map { values -> values.toMap() }

  private fun Parameter.resolveExampleEntry(exampleKey: String): Result<Pair<ParameterElement, Any?>> =
    sharedComponents
      .resolve(this)
      .flatMap { resolved ->
        sharedComponents
          .resolve(resolved.safeExamples()[exampleKey]!!)
          .map { resolved.toElement() to it.value?.normalize() }
      }

  private fun Parameter.toElement(): ParameterElement =
    when (`in`) {
      "path"   -> PathParam(name)
      "query"  -> QueryParam(name, safeAllowReserved())
      "header" -> Header(name)
      "cookie" -> Cookie(name)
      else     -> Header(name)
    }

  private fun Operation.extractExampleRequestBodies(exampleKey: String): Result<List<ScenarioBody>> =
    if (requestBody?.content == null)
      success(emptyList())
    else
      requestBody!!.content
        .filter { (_, mediaType) -> mediaType.safeExamples().containsKey(exampleKey) }
        .map { (contentType, mediaType) ->
          sharedComponents.resolve(mediaType.safeExamples()[exampleKey]!!)
            .map { it.value?.normalize() }
            .map { example -> ScenarioBody(ContentType(contentType), example) }
        }
        .combineResults()

  private fun ApiResponse.extractExampleHeaderValues(exampleKey: String): Result<Map<Header, Any?>> =
    safeHeaders()
      .filter { (_, header) -> header.safeExamples().containsKey(exampleKey) }
      .map { (name, header) ->
        sharedComponents.resolve(header.safeExamples()[exampleKey]!!)
          .map { it.value?.normalize() }
          .map { example -> Header(name) to example }
      }
      .combineResults()
      .map { values -> values.toMap() }

  private fun ApiResponse.extractExampleResponseBodies(exampleKey: String): Result<List<ScenarioBody>> =
    if (content == null)
      success(emptyList())
    else
      content
        .filter { (_, mediaType) -> mediaType.safeExamples().containsKey(exampleKey) }
        .map { (contentType, mediaType) ->
          sharedComponents.resolve(mediaType.safeExamples()[exampleKey]!!)
            .map { it.value?.normalize() }
            .map { example -> ScenarioBody(ContentType(contentType), example) }
        }
        .combineResults()

  private fun generateScenarioCombinations(path: String,
                                           method: String,
                                           statusCode: Int,
                                           key: String,
                                           examples: ExampleValues): List<Scenario> =
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

  private fun validateScenarios(key: String,
                                examples: ExampleValues,
                                requestSchema: RequestSchema,
                                responseSchema: ResponseSchema): Result<Any?> =
    validateScenarioExamples(
      key,
      ScenarioRequestExamples(examples.requestParams, examples.requestBodies, requestSchema),
      ScenarioResponseExamples(examples.responseHeaders, examples.responseBodies, responseSchema)
    )

  private fun validateScenarioExamples(exampleKey: String,
                                       request: ScenarioRequestExamples,
                                       response: ScenarioResponseExamples): Result<Any?> {
    val requestValidation = validateRequestExamples(exampleKey, request)
    val responseValidation = validateResponseExamples(exampleKey, response)
    return requestValidation combineWith responseValidation
  }

  private fun validateRequestExamples(exampleKey: String, request: ScenarioRequestExamples): Result<Any?> {
    val schemasByElement = request.schema.parameters.associateBy { it.element }
    val paramValidation = request.values.map { (element, value) ->
      when (val schema = schemasByElement[element]) {
        null -> failure("Example '$exampleKey' has no schema for request parameter '${elementName(element)}'")
        else -> schema.dataType.validate(value).forProperty(elementName(element))
      }
    }.combineResults()

    val bodyValidation = request.bodies.map { body ->
      if (body == null)
        success()
      else {
        when (val schema = request.schema.bodies.find { it.contentType.value == body.contentType.value }) {
          null -> failure("Example '$exampleKey' has no schema for request body '${body.contentType.value}'")
          else -> schema.dataType.validate(body.value).forProperty("body")
        }
      }
    }.combineResults()

    return paramValidation combineWith bodyValidation
  }

  private fun validateResponseExamples(exampleKey: String, response: ScenarioResponseExamples): Result<Any?> {
    val schemasByElement = response.schema.headers.associateBy { it.element }
    val headerValidation = response.values.map { (element, value) ->
      when (val schema = schemasByElement[element]) {
        null -> failure("Example '$exampleKey' has no schema for response header '${elementName(element)}'")
        else -> schema.dataType.validate(value).forProperty(elementName(element))
      }
    }.combineResults()

    val bodyValidation = response.bodies.map { body ->
      if (body == null)
        success()
      else {
        when (val schema = response.schema.bodies.find { it.contentType.value == body.contentType.value }) {
          null -> failure("Example '$exampleKey' has no schema for response body '${body.contentType.value}'")
          else -> schema.dataType.validate(body.value).forProperty("body")
        }
      }
    }.combineResults()

    return headerValidation combineWith bodyValidation
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
