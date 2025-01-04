package dev.blitzcraft.contracts.core.loader.swagger

import dev.blitzcraft.contracts.core.contract.Contract

data class OpenApiLoadingResult(
  val contracts: Set<Contract> = emptySet(),
  val errors: List<String> = emptyList()) {
  fun hasErrors() = errors.isNotEmpty()
}