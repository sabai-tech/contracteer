package dev.blitzcraft.contracts.core.loader.swagger

import dev.blitzcraft.contracts.core.combineResults
import io.swagger.v3.oas.models.OpenAPI

internal fun OpenAPI.generateContracts() =
  paths.flatMap { (path, item) ->
    item.readOperationsMap().flatMap { (method, operation) ->
      operation.responses.map { (code, response) ->
        SwaggerContext(path, method, operation, code, response).generateContracts()
      }
    }
  }.combineResults().map { (it ?: emptyList()).flatten() }

