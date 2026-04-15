package tech.sabai.contracteer.core.swagger

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.Result.Success
import tech.sabai.contracteer.core.combineResults
import tech.sabai.contracteer.core.operation.ApiOperation
import tech.sabai.contracteer.core.operation.ResponseSchema
import tech.sabai.contracteer.core.swagger.datatype.DataTypeConverter

internal class ApiOperationExtractor(sharedComponents: SharedComponents) {

  private val logger = KotlinLogging.logger {}
  private val dataTypeConverter = DataTypeConverter(sharedComponents)
  private val schemaExtractor = SchemaExtractor(sharedComponents, dataTypeConverter)
  private val scenarioExtractor = ScenarioExtractor(sharedComponents)

  fun extract(openAPI: OpenAPI): Result<List<ApiOperation>> {
    val equivalentPathErrors = findEquivalentPaths(openAPI)
    return when {
      equivalentPathErrors.isNotEmpty() -> failure(*equivalentPathErrors.toTypedArray())
      else                              ->
        openAPI.paths
          .flatMap { it.toApiOperations() }
          .combineResults()
          .map {
            it.mapNotNull { operation -> filterUnsupportedOperation(operation) }
              .also { operations -> logExtractedOperations(operations) }
          }
    }
  }

  private fun Map.Entry<String, PathItem>.toApiOperations() =
    value
      .readOperationsMap()
      .map { (method, operation) -> extractApiOperation(key, method.name, operation) }

  private fun extractApiOperation(path: String, method: String, operation: Operation): Result<ApiOperation> {
    val requestSchema = schemaExtractor.extractRequestSchema(operation)
    val responseSchemas = extractResponseSchemas(operation)
    val classResponses = extractClassResponses(operation)
    val defaultResponse = extractDefaultResponse(operation)

    if (!(requestSchema is Success && responseSchemas is Success && classResponses is Success && defaultResponse is Success))
      return (requestSchema combineWith responseSchemas combineWith classResponses combineWith defaultResponse)
        .mapErrors { "${method.uppercase()} $path: $it" }
        .retypeError<ApiOperation>()

    val headValidation =
      validateHeadResponses(method, responseSchemas.value, classResponses.value, defaultResponse.value)

    return when (headValidation) {
      !is Success -> headValidation.mapErrors { "${method.uppercase()} $path: $it" }.retypeError()
      else        ->
        scenarioExtractor
          .extractScenarios(path,method,operation,requestSchema.value,responseSchemas.value,classResponses.value,defaultResponse.value)
          .map {
            ApiOperation(path,method,requestSchema.value,responseSchemas.value,classResponses.value,defaultResponse.value,it)
          }
          .mapErrors { "${method.uppercase()} $path: $it" }
    }
  }

  private fun validateHeadResponses(method: String,
                                    responses: Map<Int, ResponseSchema>,
                                    classResponses: Map<Int, ResponseSchema>,
                                    defaultResponse: ResponseSchema?): Result<Unit> {
    if (!method.equals("head", ignoreCase = true)) return success()

    val allResponses = responses.values + classResponses.values + listOfNotNull(defaultResponse)
    return if (allResponses.any { it.bodies.isNotEmpty() })
      failure("HEAD responses MUST NOT include a message body (RFC 7231)")
    else
      success()
  }

  private fun extractResponseSchemas(operation: Operation): Result<Map<Int, ResponseSchema>> =
    operation.responses
      .filter { (code, _) -> code != "default" && !isClassCode(code) }
      .map { (code, response) -> schemaExtractor.extractResponseSchema(code, response) }
      .combineResults()
      .map { list -> list.associate { it.first to it.second } }

  private fun extractClassResponses(operation: Operation): Result<Map<Int, ResponseSchema>> =
    operation.responses
      .filter { (code, _) -> isClassCode(code) }
      .map { (code, response) ->
        schemaExtractor.extractResponseSchema(response).map { code[0].digitToInt() to it }
      }
      .combineResults()
      .map { list -> list.associate { it.first to it.second } }

  private fun extractDefaultResponse(operation: Operation): Result<ResponseSchema?> =
    operation.responses["default"]
      ?.let { schemaExtractor.extractResponseSchema(it) }
    ?: success(null)

  private fun findEquivalentPaths(openAPI: OpenAPI): List<String> =
    openAPI.paths.keys
      .groupBy { it.replace(PATH_PARAMETER_PATTERN, "{}") }
      .filterValues { it.size > 1 }
      .map { (_, paths) ->
        "Equivalent paths found: ${paths.joinToString(" and ") { "'$it'" }}. " +
        "These paths are identical after ignoring parameter names and are considered invalid by the OpenAPI specification (OAS 3.0 §4.7.9)."
      }

  private fun logExtractedOperations(operations: List<ApiOperation>) {
    if (operations.isEmpty()) {
      logger.warn { "No valid operations were generated." }
    } else {
      logger.info { "Found ${operations.size} valid operation(s)." }
      logger.debug {
        operations.joinToString("${System.lineSeparator()}- ", "Operations:${System.lineSeparator()}- ") { operation ->
          val responseCodes = operation.responses.keys.sorted().joinToString(", ")
          "${operation.method.uppercase()} ${operation.path} -> [$responseCodes]"
        }
      }
    }
  }

  companion object {
    private val PATH_PARAMETER_PATTERN = Regex("\\{[^}]+}")
  }
}
