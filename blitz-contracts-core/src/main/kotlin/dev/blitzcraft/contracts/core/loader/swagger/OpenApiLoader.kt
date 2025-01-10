package dev.blitzcraft.contracts.core.loader.swagger

import dev.blitzcraft.contracts.core.Result
import dev.blitzcraft.contracts.core.Result.Companion.failure
import dev.blitzcraft.contracts.core.Result.Companion.success
import dev.blitzcraft.contracts.core.accumulate
import dev.blitzcraft.contracts.core.contract.*
import dev.blitzcraft.contracts.core.sequence
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.ParseOptions
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString

fun File.generateContracts() = toPath().generateContracts()

fun Path.generateContracts(): Result<List<Contract>> {
  val parseResult = OpenAPIV3Parser().readLocation(absolutePathString(),
                                                   emptyList(),
                                                   ParseOptions().apply { isResolve = true })

  if (parseResult.messages.isNotEmpty()) return failure(*parseResult.messages.toTypedArray())
  SharedComponents.components = parseResult.openAPI.components

  return checkFor2xxResponses(parseResult.openAPI)
    .mapSuccess { validateExamples(parseResult.openAPI.contracts()).sequence() }
}

private fun checkFor2xxResponses(openAPI: OpenAPI): Result<OpenAPI> {
  val errors = openAPI.paths.flatMap { (path, item) ->
    item.readOperationsMap()
      .filter { (_, operation) -> operation.responses.none { it.key.startsWith("2") } }
      .map { "'${path}: ${item.summary}' does not contain response for 2xx" }
  }
  return when {
    errors.isEmpty() -> success(openAPI)
    else             -> failure(*errors.toTypedArray())
  }
}


private fun validateExamples(contracts: Set<Contract>): List<Result<Contract>> =
  contracts
    .map { contract ->
      val result = validateRequestExample(contract.request) combineWith validateResponseExample(contract.response)
      if (result.isSuccess()) result.mapSuccess { success(contract) }
      else result.mapErrors { "${contract.description()}, $it" }.retypeError()
    }

private fun validateRequestExample(request: ContractRequest) =
  (request.headers.validateExamplesWithContext("request header") combineWith
      request.pathParameters.validateExamplesWithContext("request path parameter") combineWith
      request.cookies.validateExamplesWithContext("request cookie") combineWith
      request.queryParameters.validateExamplesWithContext("request query parameter") combineWith
      request.body.validateExampleWithContext("request body")
  ).mapSuccess { success(request) }

private fun validateResponseExample(response: ContractResponse) =
  (response.headers.validateExamplesWithContext("response header") combineWith
      response.body.validateExampleWithContext("response body")).mapSuccess { success(response) }

private fun Collection<ContractParameter>.validateExamplesWithContext(context: String) =
  filter { it.hasExample() }
    .accumulate { it.dataType.validate(it.example!!.normalizedValue).forProperty(it.name) }
    .mapErrors { "$context $it" }

private fun Body?.validateExampleWithContext(context: String) =
  if (this != null && hasExample()) dataType.validate(example!!.normalizedValue).mapErrors { "$context $it" }
  else success()
