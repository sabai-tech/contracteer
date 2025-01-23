package dev.blitzcraft.contracts.core.loader.swagger

import dev.blitzcraft.contracts.core.Result
import dev.blitzcraft.contracts.core.Result.Companion.failure
import dev.blitzcraft.contracts.core.Result.Companion.success
import dev.blitzcraft.contracts.core.contract.Contract
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.ParseOptions
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString

fun File.loadContracts() = toPath().loadContracts()

fun Path.loadContracts(): Result<List<Contract>> {
  val parseResult = OpenAPIV3Parser().readLocation(absolutePathString(),
                                                   emptyList(),
                                                   ParseOptions().apply { isResolve = true })

  if (parseResult.messages.isNotEmpty()) return failure(*parseResult.messages.toTypedArray())

  SharedComponents.components = parseResult.openAPI.components
  return checkFor2xxResponses(parseResult.openAPI) andThen { parseResult.openAPI.generateContracts() }
}

private fun checkFor2xxResponses(openAPI: OpenAPI): Result<OpenAPI> =
  openAPI.paths
    .flatMap { (path, item) ->
      item.readOperationsMap()
        .filter { (_, operation) -> operation.responses.none { it.key.startsWith("2") } }
        .map { "'${it.key} ${path}: ' does not contain response for 2xx" }
    }.let {
      if (it.isEmpty()) success(openAPI)
      else failure(*it.toTypedArray())
    }
