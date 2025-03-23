package tech.sabai.contracteer.core.swagger

import io.swagger.v3.oas.models.responses.ApiResponse
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success


object ApiResponseResolver {
  private const val COMPONENTS_RESPONSE_BASE_REF = "#/components/responses/"
  private const val MAX_RECURSIVE_DEPTH = 10

  internal lateinit var sharedApiResponses: Map<String, ApiResponse>

  fun resolve(response: ApiResponse, maxRecursiveDepth: Int = MAX_RECURSIVE_DEPTH): Result<ApiResponse> {
    val ref = response.shortRef()
    return when {
      maxRecursiveDepth < 0                   -> failure("Max recursive depth reached for Api Response")
      ref == null                             -> success(response)
      sharedApiResponses[ref]?.`$ref` != null -> resolve(sharedApiResponses[ref]!!, maxRecursiveDepth - 1)
      sharedApiResponses[ref] != null         -> success(sharedApiResponses[ref]!!)
      else                                    -> failure("Response '${response.`$ref`}' not found")
    }
  }

  private fun ApiResponse.shortRef() =
    this.`$ref`?.replace(COMPONENTS_RESPONSE_BASE_REF, "")
}
