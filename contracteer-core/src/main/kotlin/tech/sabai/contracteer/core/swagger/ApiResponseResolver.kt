package tech.sabai.contracteer.core.swagger

import io.swagger.v3.oas.models.responses.ApiResponse
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success


object ApiResponseResolver {
  private const val COMPONENTS_RESPONSE_BASE_REF = "#/components/responses/"

  internal lateinit var sharedApiResponses: Map<String, ApiResponse>

  fun resolveResponse(response: ApiResponse) =
    when {
      sharedApiResponses.isEmpty() && response.`$ref` != null -> failure("'components' section is not defined")
      response.`$ref` != null                                 ->
        sharedApiResponses[response.shortRef()]?.let { success(it) }
        ?: failure("Response '${response.`$ref`}' not found")
      else                                                    -> success(response)
    }

  private fun ApiResponse.shortRef() =
    this.`$ref`?.replace(COMPONENTS_RESPONSE_BASE_REF, "")
}
