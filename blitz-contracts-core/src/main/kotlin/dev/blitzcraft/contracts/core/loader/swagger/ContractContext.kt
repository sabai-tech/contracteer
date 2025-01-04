package dev.blitzcraft.contracts.core.loader.swagger

import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem.HttpMethod
import io.swagger.v3.oas.models.responses.ApiResponse

data class ContractContext(
  val path: String,
  val method: HttpMethod,
  val operation: Operation,
  val statusCode: Int,
  val apiResponse: ApiResponse
)