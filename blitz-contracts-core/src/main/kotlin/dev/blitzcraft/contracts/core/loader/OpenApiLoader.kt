package dev.blitzcraft.contracts.core.loader

import dev.blitzcraft.contracts.core.contract.Contract
import dev.blitzcraft.contracts.core.convert
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.ParseOptions
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString

fun File.loadOpenApiSpec() = toPath().loadOpenApiSpec()

fun Path.loadOpenApiSpec(): OpenApiLoadingResult {
  val parseResult = OpenAPIV3Parser().readLocation(absolutePathString(),
                                                   emptyList(),
                                                   ParseOptions().apply { isResolveFully = true })

  if (parseResult.messages.isNotEmpty()) return OpenApiLoadingResult(errors = parseResult.messages)

  val errors = checkFor2xxResponses(parseResult.openAPI) + validateExamples(parseResult.openAPI)

  return if (errors.isEmpty()) OpenApiLoadingResult(parseResult.openAPI.contracts()) else OpenApiLoadingResult(errors = errors)
}

data class OpenApiLoadingResult(
  val contracts: Set<Contract> = emptySet(),
  val errors: List<String> = emptyList()) {
  fun hasErrors() = errors.isNotEmpty()
}

private fun checkFor2xxResponses(openAPI: OpenAPI) =
  openAPI.paths.flatMap { pathAndItem ->
    pathAndItem.item()
      .readOperationsMap()
      .filter { methodAndOperation ->
        methodAndOperation.operation().responses.filter { it.code().startsWith("2") }.isEmpty()
      }.map { "'${it.method()}: ${pathAndItem.path()}' does not contain response for 2xx" }
  }

private fun validateExamples(openAPI: OpenAPI) =
  openAPI.paths.flatMap { pathItem ->
    pathItem.item().readOperationsMap()
      .flatMap {
        (it.validateRequestExamples() + it.validateResponseExamples()).map { message -> "method: ${it.method()}, $message" }
      }
      .map { "path: ${pathItem.path()}, $it" }
  }.sorted()

private fun Map.Entry<PathItem.HttpMethod, Operation>.validateRequestExamples() =
  operation().validateRequestParameterExamples() + operation().validateRequestBodyExamples()

private fun Operation.validateRequestBodyExamples() =
  requestBody?.content?.flatMap { contentAndMediaType ->
    contentAndMediaType.mediaType().safeExamples().flatMap { namedExample ->
      contentAndMediaType.mediaType().schema
        .toDataType()
        .validate(namedExample.example().value.convert())
        .errors()
        .map {
          "request body: ${contentAndMediaType.content()}, example: ${namedExample.name()} -> $it"
        }
    }
  } ?: emptyList()

private fun Operation.validateRequestParameterExamples() =
  safeParameters().flatMap { parameter ->
    parameter.safeExamples().flatMap { namedExample ->
      parameter.schema.toDataType().validate(namedExample.example().value.convert()).errors().map {
        "request parameter: ${parameter.name}, example: ${namedExample.name()} -> $it"
      }
    }
  }

private fun Map.Entry<PathItem.HttpMethod, Operation>.validateResponseExamples() =
  operation().responses.flatMap { it.validateHeaderExamples() + it.validateContentExamples() }

private fun Map.Entry<String, ApiResponse>.validateContentExamples() =
  response().content
    ?.flatMap { contentAndMediaType ->
      contentAndMediaType.mediaType().safeExamples().flatMap { namedExample ->
        contentAndMediaType.mediaType().schema.toDataType().validate(namedExample.example().value.convert()).errors()
          .map { "content: ${contentAndMediaType.content()}, example: ${namedExample.name()} -> $it" }
      }
    }
    ?.map { "response status code: $key, $it" }
  ?: emptyList()

private fun Map.Entry<String, ApiResponse>.validateHeaderExamples() =
  response().safeHeaders().flatMap { header ->
    header.value.safeExamples().flatMap { namedExample ->
      header.value.schema.toDataType().validate(namedExample.example().value.convert()).errors().map {
        "header: ${header.key}, example: ${namedExample.name()} -> $it"
      }
    }
  }.map { "response status code: $key, $it" }