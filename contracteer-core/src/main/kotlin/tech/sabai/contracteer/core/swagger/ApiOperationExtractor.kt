package tech.sabai.contracteer.core.swagger

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.combineResults
import tech.sabai.contracteer.core.operation.ApiOperation
import tech.sabai.contracteer.core.operation.ResponseSchema
import tech.sabai.contracteer.core.swagger.datatype.DataTypeConverter

internal class ApiOperationExtractor(private val sharedComponents: SharedComponents) {

  private val logger = KotlinLogging.logger {}
  private val dataTypeConverter = DataTypeConverter(sharedComponents)
  private val schemaExtractor = SchemaExtractor(sharedComponents, dataTypeConverter)
  private val scenarioExtractor = ScenarioExtractor(sharedComponents)

  fun extract(openAPI: OpenAPI): Result<List<ApiOperation>> =
    openAPI.paths
      .flatMap { it.toApiOperations() }
      .combineResults()
      .map {
        (it ?: emptyList())
          .mapNotNull { operation -> filterUnsupportedOperation(operation) }
          .also { operations -> logExtractedOperations(operations) }
      }

  private fun Map.Entry<String, PathItem>.toApiOperations() =
    value
      .readOperationsMap()
      .map { (method, operation) -> extractApiOperation(key, method.name, operation) }

  private fun extractApiOperation(
    path: String,
    method: String,
    operation: Operation
  ): Result<ApiOperation> {
    val requestSchema = schemaExtractor.extractRequestSchema(operation)
    val responseSchemas = extractResponseSchemas(operation)
    val classResponses = extractClassResponses(operation)
    val defaultResponse = extractDefaultResponse(operation)

    if (!allAreSuccess(requestSchema, responseSchemas, classResponses, defaultResponse))
      return requestSchema.retypeError<ApiOperation>() combineWith
          responseSchemas.retypeError() combineWith
          classResponses.retypeError() combineWith
          defaultResponse.retypeError()

    val scenarios = scenarioExtractor.extractScenarios(path, method, operation, requestSchema, responseSchemas)
    if (scenarios.isFailure()) return scenarios.retypeError()

    return success(ApiOperation(path,
                                method,
                                requestSchema.value!!,
                                responseSchemas.value!!,
                                classResponses.value!!,
                                defaultResponse.value,
                                scenarios.value!!))
  }

  private fun extractResponseSchemas(operation: Operation): Result<Map<Int, ResponseSchema>> =
    operation.responses
      .filter { (code, _) -> code != "default" && !isClassCode(code) }
      .map { (code, response) -> schemaExtractor.extractResponseSchema(code, response) }
      .combineResults()
      .map { list -> list?.associate { it.first to it.second } ?: emptyMap() }

  private fun extractClassResponses(operation: Operation): Result<Map<Int, ResponseSchema>> =
    operation.responses
      .filter { (code, _) -> isClassCode(code) }
      .map { (code, response) ->
        schemaExtractor.extractResponseSchema(response).map { code[0].digitToInt() to it!! }
      }
      .combineResults()
      .map { list -> list?.associate { it.first to it.second } ?: emptyMap() }

  private fun extractDefaultResponse(operation: Operation): Result<ResponseSchema?> =
    operation.responses["default"]
      ?.let { schemaExtractor.extractResponseSchema(it) }
    ?: success(null)

  private fun isClassCode(code: String): Boolean =
    code.length == 3 && code[0].isDigit() && code.substring(1).uppercase() == "XX"

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
}
