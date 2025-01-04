package dev.blitzcraft.contracts.core.loader.swagger

import io.swagger.v3.oas.models.OpenAPI

internal fun OpenAPI.contracts() =
  paths.flatMap { (path, item) ->
    item.readOperationsMap().flatMap { (method, operation) ->
      operation.responses.flatMap { (code, response) ->
        val context = ContractContext(path, method, operation, code.toInt(), response)
        val contractsWithExample = extractExampleBasedContracts(context)

        if (code.startsWith("2") && contractsWithExample.isEmpty())
          extractDefaultSuccessContracts(context)
        else
          contractsWithExample
      }
    }
  }.toSet()