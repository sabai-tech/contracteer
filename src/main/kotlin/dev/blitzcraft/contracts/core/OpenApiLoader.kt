package dev.blitzcraft.contracts.core

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.ParseOptions
import java.nio.file.Path
import kotlin.io.path.absolutePathString

object OpenApiLoader {
  fun from(path: Path): OpenApiLoadingResult {
    val parseResult = OpenAPIV3Parser().readLocation(path.absolutePathString(),
                                                     emptyList(),
                                                     ParseOptions().also { it.isResolveFully = true })

    if (parseResult.messages.isNotEmpty()) return OpenApiLoadingResult(errors = parseResult.messages)
    val errors = checkFor2xxResponses(parseResult.openAPI)
    return if (errors.isEmpty())
      OpenApiLoadingResult(parseResult.openAPI)
    else
      OpenApiLoadingResult(errors = errors)
  }

  private fun checkFor2xxResponses(openAPI: OpenAPI) =
    openAPI.paths.flatMap { pathAndItem ->
      pathAndItem.item()
        .readOperationsMap()
        .filter { methodAndOperation ->
          methodAndOperation.operation().responses.filter { it.code().startsWith("2") }.isEmpty()
        }.map { "'${it.method()}: ${pathAndItem.path()}' does not contain response for 2xx" }
    }
}

data class OpenApiLoadingResult(val openAPI: OpenAPI? = null, val errors: List<String> = emptyList())