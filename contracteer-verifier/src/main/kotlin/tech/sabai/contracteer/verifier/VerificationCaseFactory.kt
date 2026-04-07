package tech.sabai.contracteer.verifier

import io.github.oshai.kotlinlogging.KotlinLogging
import tech.sabai.contracteer.core.codec.DeepObjectParameterCodec
import tech.sabai.contracteer.core.datatype.ObjectDataType
import tech.sabai.contracteer.core.operation.*
import tech.sabai.contracteer.verifier.VerificationCase.*

/**
 * Generates [VerificationCase] instances from an [ApiOperation].
 *
 * Produces three kinds of verification cases:
 * - [VerificationCase.ScenarioBased]: one per scenario defined in the operation
 * - [VerificationCase.SchemaBased]: generated from the schema when no 2xx scenario exists
 * - [VerificationCase.TypeMismatch]: generated for 400 Bad Request validation when a 400 response is defined
 */
object VerificationCaseFactory {
  private val logger = KotlinLogging.logger {}

  /** Creates all verification cases for the given [apiOperation]. */
  fun create(apiOperation: ApiOperation): List<VerificationCase> {
    val scenarioCases = createScenarioBasedCases(apiOperation)
    val schemaBasedCases = createSchemaBasedCasesIfNeeded(apiOperation)
    val typeMismatchCases = createTypeMismatchs(apiOperation)

    return scenarioCases + schemaBasedCases + typeMismatchCases
  }

  private fun createScenarioBasedCases(apiOperation: ApiOperation): List<ScenarioBased> {
    return apiOperation.scenarios.map { scenario ->
      val responseSchema = apiOperation.responseFor(scenario.statusCode)
                           ?: error("No response schema found for status code ${scenario.statusCode} in operation ${apiOperation.method} ${apiOperation.path}")

      ScenarioBased(
        scenario = scenario,
        requestSchema = apiOperation.requestSchema,
        responseSchema = responseSchema
      )
    }
  }

  private fun createSchemaBasedCasesIfNeeded(apiOperation: ApiOperation): List<SchemaBased> {
    if (hasSuccessScenario(apiOperation)) return emptyList()

    val successResponses = apiOperation.successResponses()

    return when {
      successResponses.isEmpty() -> emptyList()
      successResponses.size > 1  -> {
        logger.warn {
          "Operation ${apiOperation.method} ${apiOperation.path} has multiple 2xx responses " +
          "(${successResponses.keys.sorted().joinToString(", ")}) but no scenario. " +
          "Skipping schema-based verification generation."
        }
        emptyList()
      }
      else                       -> {
        val (statusCode, responseSchema) = successResponses.entries.first()
        createSchemaBasedCases(apiOperation, statusCode, responseSchema)
      }
    }
  }

  private fun hasSuccessScenario(apiOperation: ApiOperation): Boolean {
    return apiOperation.scenarios.any { isSuccessStatusCode(it.statusCode) }
  }

  private fun isSuccessStatusCode(statusCode: Int): Boolean {
    return statusCode in 200..299
  }

  private fun createTypeMismatchs(apiOperation: ApiOperation): List<TypeMismatch> {
    val responseSchema = apiOperation.badRequestResponse() ?: return emptyList()
    val responseContentType = responseSchema.bodies.firstOrNull()?.contentType
    val requestSchema = apiOperation.requestSchema

    val cases = listOfNotNull(
      createParameterTypeMismatch(apiOperation, responseSchema, responseContentType, requestSchema.pathParameters),
      createParameterTypeMismatch(apiOperation, responseSchema, responseContentType, requestSchema.queryParameters),
      createParameterTypeMismatch(apiOperation, responseSchema, responseContentType, requestSchema.headers),
      createParameterTypeMismatch(apiOperation, responseSchema, responseContentType, requestSchema.cookies),
      createBodyTypeMismatch(apiOperation, responseSchema, responseContentType)
    )

    if (cases.isEmpty()) {
      logger.warn {
        "Operation ${apiOperation.method} ${apiOperation.path} defines a 400 response but no type mismatch " +
        "verification case could be generated (all request elements are non-mutable types such as string)."
      }
    }

    return cases
  }

  private fun createParameterTypeMismatch(
    apiOperation: ApiOperation,
    responseSchema: ResponseSchema,
    responseContentType: ContentType?,
    parameters: List<ParameterSchema>
  ): TypeMismatch? {
    val (param, mutatedValue) = findFirstMutableParameter(parameters) ?: return null

    return TypeMismatch(
      path = apiOperation.path,
      method = apiOperation.method,
      requestContentType = null,
      responseContentType = responseContentType,
      requestSchema = apiOperation.requestSchema,
      responseSchema = responseSchema,
      mutatedElement = MutatedElement.Parameter(param.element),
      mutatedValue = mutatedValue
    )
  }

  private fun findFirstMutableParameter(parameters: List<ParameterSchema>): Pair<ParameterSchema, String>? =
    parameters
      .filter { !it.isDeepObjectWithAllOptionalProperties() }
      .firstNotNullOfOrNull { param ->
        TypeMismatchMutation.mutate(param.dataType)?.let { mutated -> param to mutated }
      }

  private fun ParameterSchema.isDeepObjectWithAllOptionalProperties(): Boolean {
    if (codec !is DeepObjectParameterCodec) return false
    val objectType = dataType as? ObjectDataType ?: return false
    return objectType.requiredProperties.isEmpty() && objectType.allowAdditionalProperties
  }

  private fun createBodyTypeMismatch(
    apiOperation: ApiOperation,
    responseSchema: ResponseSchema,
    responseContentType: ContentType?
  ): TypeMismatch? {
    val mutableBody = findFirstMutableBody(apiOperation.requestSchema.bodies) ?: return null

    return TypeMismatch(
      path = apiOperation.path,
      method = apiOperation.method,
      requestContentType = mutableBody.first.contentType,
      responseContentType = responseContentType,
      requestSchema = apiOperation.requestSchema,
      responseSchema = responseSchema,
      mutatedElement = MutatedElement.Body,
      mutatedValue = mutableBody.second
    )
  }

  private fun findFirstMutableBody(bodies: List<BodySchema>): Pair<BodySchema, String>? =
    bodies
      .filter { !it.isFormWithAllOptionalProperties() }
      .firstNotNullOfOrNull { body ->
        TypeMismatchMutation.mutate(body.dataType)?.let { mutated -> body to mutated }
      }

  private fun BodySchema.isFormWithAllOptionalProperties(): Boolean {
    if (!contentType.isFormUrlEncoded()) return false
    val objectType = dataType as? ObjectDataType ?: return false
    return objectType.requiredProperties.isEmpty() && objectType.allowAdditionalProperties
  }

  private fun createSchemaBasedCases(
    apiOperation: ApiOperation,
    statusCode: Int,
    responseSchema: ResponseSchema
  ): List<SchemaBased> {
    val requestBodies = apiOperation.requestSchema.bodies.ifEmpty { listOf(null) }
    val responseBodies = responseSchema.bodies.ifEmpty { listOf(null) }

    return requestBodies.flatMap { requestBody ->
      responseBodies.map { responseBody ->
        SchemaBased(
          path = apiOperation.path,
          method = apiOperation.method,
          statusCode = statusCode,
          requestContentType = requestBody?.contentType,
          responseContentType = responseBody?.contentType,
          requestSchema = apiOperation.requestSchema,
          responseSchema = responseSchema
        )
      }
    }
  }
}
