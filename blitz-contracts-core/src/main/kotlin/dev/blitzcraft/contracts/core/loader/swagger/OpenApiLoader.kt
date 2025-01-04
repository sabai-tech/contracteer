package dev.blitzcraft.contracts.core.loader.swagger

import dev.blitzcraft.contracts.core.contract.*
import dev.blitzcraft.contracts.core.validation.validateEach
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.ParseOptions
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString

fun File.loadOpenApiSpec() = toPath().loadOpenApiSpec()

fun Path.loadOpenApiSpec(): OpenApiLoadingResult {
  val parseResult = OpenAPIV3Parser().readLocation(absolutePathString(),
                                                   emptyList(),
                                                   ParseOptions().apply { isResolve = true })

  if (parseResult.messages.isNotEmpty()) return OpenApiLoadingResult(errors = parseResult.messages)
  SharedComponents.components = parseResult.openAPI.components

  val missing2xxErrors = checkFor2xxResponses(parseResult.openAPI)
  if (missing2xxErrors.isNotEmpty()) return OpenApiLoadingResult(errors = missing2xxErrors)

  val examplesErrors = validateExamples(parseResult.openAPI.contracts())
  if (examplesErrors.isNotEmpty()) return OpenApiLoadingResult(errors = examplesErrors)

  return OpenApiLoadingResult(parseResult.openAPI.contracts())
}

private fun checkFor2xxResponses(openAPI: OpenAPI) =
  openAPI.paths.flatMap { (path, item) ->
    item.readOperationsMap()
      .filter { (_, operation) -> operation.responses.none { it.key.startsWith("2") } }
      .map { "'${path}: ${item.summary}' does not contain response for 2xx" }
  }

private fun validateExamples(contracts: Set<Contract>) =
  contracts
    .filter { it.hasExample() }
    .flatMap { contract ->
      (validateRequestExample(contract.request) +
       validateResponseExample(contract.response)
      ).map { "${contract.description()}, $it" }
    }

private fun validateRequestExample(request: ContractRequest) =
  request.headers.validateExamplesWithContext("request header") +
  request.pathParameters.validateExamplesWithContext("request path parameter") +
  request.cookies.validateExamplesWithContext("request cookie") +
  request.queryParameters.validateExamplesWithContext("request query parameter") +
  request.body.validateExampleWithContext("request body")

private fun validateResponseExample(response: ContractResponse) =
  response.headers.validateExamplesWithContext("response header") +
  response.body.validateExampleWithContext("response body")

private fun Collection<ContractParameter>.validateExamplesWithContext(context: String) =
  filter { it.hasExample() }
    .validateEach { it.dataType.validate(it.example!!.normalizedValue).forProperty(it.name) }
    .errors()
    .map { "$context $it" }

private fun Body?.validateExampleWithContext(context: String) =
  if (this != null && hasExample()) dataType.validate(example!!.normalizedValue).errors().map { "$context $it" }
  else emptyList()
