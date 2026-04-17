package tech.sabai.contracteer.core.swagger

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.Result.Failure
import tech.sabai.contracteer.core.Result.Success
import tech.sabai.contracteer.core.combineResults
import tech.sabai.contracteer.core.operation.ApiOperation
import tech.sabai.contracteer.core.operation.ResponseSchemas
import tech.sabai.contracteer.core.swagger.datatype.DataTypeConverter

internal class ApiOperationExtractor(sharedComponents: SharedComponents) {

  private val logger = KotlinLogging.logger {}
  private val dataTypeConverter = DataTypeConverter(sharedComponents)
  private val schemaExtractor = SchemaExtractor(sharedComponents, dataTypeConverter)

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
      .map { (method, operation) -> extractApiOperation(method.name, key, operation) }

  private fun extractApiOperation(method: String, path: String, operation: Operation): Result<ApiOperation> {
    val extractedRequest = schemaExtractor.extractRequestSchema(operation)
    val extractedByStatusCode = extractResponseSchemas(operation)
    val extractedByClass = extractClassResponses(operation)
    val extractedDefault = extractDefaultResponse(operation)

    if (!(extractedRequest is Success && extractedByStatusCode is Success && extractedByClass is Success && extractedDefault is Success))
      return (extractedRequest combineWith extractedByStatusCode combineWith extractedByClass combineWith extractedDefault)
        .mapErrors { "${method.uppercase()} $path: $it" }
        .retypeError<ApiOperation>()

    val requestSchema = extractedRequest.value.toRequestSchema()
    val responseSchemas = ResponseSchemas(
      extractedByStatusCode.value.mapValues { it.value.toResponseSchema() },
      extractedByClass.value.mapValues { it.value.toResponseSchema() },
      extractedDefault.value?.toResponseSchema()
    )

    return when (val headValidation = validateHeadResponses(method, responseSchemas)) {
      is Failure -> headValidation.mapErrors { "${method.uppercase()} $path: $it" }.retypeError()
      else       ->
        ScenarioBuilder
          .buildScenarios(method,
                          path,
                          extractedRequest.value,
                          extractedByStatusCode.value,
                          extractedByClass.value,
                          extractedDefault.value)
          .map { scenarios -> ApiOperation(path, method, requestSchema, responseSchemas, scenarios) }
          .mapErrors { "${method.uppercase()} $path: $it" }
    }
  }

  private fun validateHeadResponses(method: String, responseSchemas: ResponseSchemas): Result<Unit> {
    if (!method.equals("head", ignoreCase = true)) return success()

    return when {
      responseSchemas.hasAnyBody() -> failure("HEAD responses MUST NOT include a message body (RFC 7231)")
      else                         -> success()
    }
  }

  private fun extractResponseSchemas(operation: Operation): Result<Map<Int, ExtractedResponseSchema>> =
    operation.responses
      .filter { (code, _) -> code != "default" && !isClassCode(code) }
      .map { (code, response) -> schemaExtractor.extractResponseSchema(code, response) }
      .combineResults()
      .map { list -> list.associate { it.first to it.second } }

  private fun extractClassResponses(operation: Operation): Result<Map<Int, ExtractedResponseSchema>> =
    operation.responses
      .filter { (code, _) -> isClassCode(code) }
      .map { (code, response) ->
        schemaExtractor.extractResponseSchema(response).map { code[0].digitToInt() to it }
      }
      .combineResults()
      .map { list -> list.associate { it.first to it.second } }

  private fun extractDefaultResponse(operation: Operation): Result<ExtractedResponseSchema?> =
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
          "${operation.method.uppercase()} ${operation.path} -> [${operation.responseSchemas.summary()}]"
        }
      }
    }
  }

  companion object {
    private val PATH_PARAMETER_PATTERN = Regex("\\{[^}]+}")
  }
}
