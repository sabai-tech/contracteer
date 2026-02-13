package tech.sabai.contracteer.verifier

import io.github.oshai.kotlinlogging.KotlinLogging
import tech.sabai.contracteer.core.operation.ApiOperation
import tech.sabai.contracteer.core.operation.BodySchema
import tech.sabai.contracteer.core.operation.ContentType
import tech.sabai.contracteer.core.operation.ParameterSchema
import tech.sabai.contracteer.core.operation.ResponseSchema
import tech.sabai.contracteer.verifier.VerificationCase.*

object VerificationCaseFactory {
  private val logger = KotlinLogging.logger {}

  fun create(apiOperation: ApiOperation): List<VerificationCase> {
    val scenarioCases = createScenarioBasedCases(apiOperation)
    val schemaBasedCases = createSchemaBasedCasesIfNeeded(apiOperation)
    val typeMismatchCases = createTypeMismatchCases(apiOperation)

    return scenarioCases + schemaBasedCases + typeMismatchCases
  }

  private fun createScenarioBasedCases(apiOperation: ApiOperation): List<ScenarioBased> {
    return apiOperation.scenarios.map { scenario ->
      val responseSchema = apiOperation.responses[scenario.statusCode]
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

    val successResponses = apiOperation.responses.filterKeys { isSuccessStatusCode(it) }

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

  private fun createTypeMismatchCases(apiOperation: ApiOperation): List<TypeMismatchCase> {
    val responseSchema = apiOperation.responses[400] ?: return emptyList()
    val responseContentType = responseSchema.bodies.firstOrNull()?.contentType
    val requestSchema = apiOperation.requestSchema

    return listOfNotNull(
      createParameterTypeMismatchCase(apiOperation, responseSchema, responseContentType, requestSchema.pathParameters),
      createParameterTypeMismatchCase(apiOperation, responseSchema, responseContentType, requestSchema.queryParameters),
      createParameterTypeMismatchCase(apiOperation, responseSchema, responseContentType, requestSchema.headers),
      createParameterTypeMismatchCase(apiOperation, responseSchema, responseContentType, requestSchema.cookies),
      createBodyTypeMismatchCase(apiOperation, responseSchema, responseContentType)
    )
  }

  private fun createParameterTypeMismatchCase(
    apiOperation: ApiOperation,
    responseSchema: ResponseSchema,
    responseContentType: ContentType?,
    parameters: List<ParameterSchema>
  ): TypeMismatchCase? {
    val (param, mutatedValue) = findFirstMutableParameter(parameters) ?: return null

    return TypeMismatchCase(
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
    parameters.firstNotNullOfOrNull { param ->
      TypeMismatchMutation.mutate(param.dataType)?.let { mutated -> param to mutated }
    }

  private fun createBodyTypeMismatchCase(
    apiOperation: ApiOperation,
    responseSchema: ResponseSchema,
    responseContentType: ContentType?
  ): TypeMismatchCase? {
    val mutableBody = findFirstMutableBody(apiOperation.requestSchema.bodies) ?: return null

    return TypeMismatchCase(
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
    bodies.firstNotNullOfOrNull { body ->
      TypeMismatchMutation.mutate(body.dataType)?.let { mutated -> body to mutated }
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
