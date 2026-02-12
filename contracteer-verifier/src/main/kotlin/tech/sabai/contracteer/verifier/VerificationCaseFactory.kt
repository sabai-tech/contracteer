package tech.sabai.contracteer.verifier

import io.github.oshai.kotlinlogging.KotlinLogging
import tech.sabai.contracteer.core.operation.ApiOperation
import tech.sabai.contracteer.core.operation.ResponseSchema
import tech.sabai.contracteer.verifier.VerificationCase.*

object VerificationCaseFactory {
  private val logger = KotlinLogging.logger {}

  fun create(apiOperation: ApiOperation): List<VerificationCase> {
    val scenarioCases = createScenarioBasedCases(apiOperation)
    val schemaBasedCases = createSchemaBasedCasesIfNeeded(apiOperation)

    return scenarioCases + schemaBasedCases
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
