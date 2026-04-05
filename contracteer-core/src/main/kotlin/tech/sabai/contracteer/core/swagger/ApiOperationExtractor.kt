package tech.sabai.contracteer.core.swagger

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import tech.sabai.contracteer.core.Result
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

  fun extract(openAPI: OpenAPI): Result<List<ApiOperation>> =
    openAPI.paths
      .flatMap { it.toApiOperations() }
      .combineResults()
      .map {
        it.mapNotNull { operation -> filterUnsupportedOperation(operation) }
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

    if (!(requestSchema is Success && responseSchemas is Success && classResponses is Success && defaultResponse is Success))
      return (requestSchema combineWith
          responseSchemas combineWith
          classResponses combineWith
          defaultResponse).retypeError()

    return scenarioExtractor
      .extractScenarios(path,method,operation,requestSchema.value,responseSchemas.value,classResponses.value,defaultResponse.value)
      .map {
        ApiOperation(path,method,requestSchema.value,responseSchemas.value,classResponses.value,defaultResponse.value,it)
      }
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
