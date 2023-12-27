package dev.blitzcraft.contracts.core

import dev.blitzcraft.contracts.core.datatype.DataType
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.ParseOptions
import java.nio.file.Path
import kotlin.io.path.absolutePathString

internal object OpenApiLoader {
  fun from(path: Path): OpenApiLoadingResult {
    val parseResult = OpenAPIV3Parser().readLocation(path.absolutePathString(),
                                                     emptyList(),
                                                     ParseOptions().also { it.isResolveFully = true })

    if (parseResult.messages.isNotEmpty()) return OpenApiLoadingResult(errors = parseResult.messages)

    val errors = checkFor2xxResponses(parseResult.openAPI) + validateExamples(parseResult.openAPI)
    return if (errors.isEmpty()) OpenApiLoadingResult(parseResult.openAPI)
    else OpenApiLoadingResult(errors = errors)
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
    value.validateRequestParameterExamples() + value.validateRequestBodyExamples()

  private fun Operation.validateRequestBodyExamples() =
    requestBody?.content?.flatMap { contentAndMediaType ->
      contentAndMediaType.mediaType().safeExamples().flatMap { example ->
        DataType.from(contentAndMediaType.mediaType().schema).validateValue(example.value.value).errors().map {
          "request body: ${contentAndMediaType.content()}, example: ${example.key} -> $it"
        }
      }
    } ?: emptyList()

  private fun Operation.validateRequestParameterExamples() =
    safeParameters().flatMap { parameter ->
      parameter.safeExamples().flatMap { example ->
        DataType.from(parameter.schema).validateValue(example.value.value).errors().map {
          "request parameter: ${parameter.name}, example: ${example.key} -> $it"
        }
      }
    }

  private fun Map.Entry<PathItem.HttpMethod, Operation>.validateResponseExamples() =
    value.responses.flatMap { it.validateHeaderExamples() + it.validateContentExamples() }

  private fun Map.Entry<String, ApiResponse>.validateContentExamples() =
    (value.content ?: Content()).flatMap { contentAndMediaType ->
      contentAndMediaType.mediaType().safeExamples().flatMap { example ->
        DataType.from(contentAndMediaType.mediaType().schema).validateValue(example.value.value).errors().map {
          "content: ${contentAndMediaType.content()}, example: ${example.key} -> $it"
        }
      }
    }.map { "response status code: $key, $it" }

  private fun Map.Entry<String, ApiResponse>.validateHeaderExamples() =
    value.safeHeaders().flatMap { header ->
      header.value.safeExamples().flatMap { example ->
        DataType.from(header.value.schema).validateValue(example.value.value).errors().map {
          "header: ${header.key}, example: ${example.key} -> $it"
        }
      }
    }.map { "response status code: $key, $it" }
}

internal data class OpenApiLoadingResult(val openAPI: OpenAPI? = null, val errors: List<String> = emptyList())