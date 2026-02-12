package tech.sabai.contracteer.verifier

import tech.sabai.contracteer.core.operation.ContentType
import tech.sabai.contracteer.core.operation.RequestSchema
import tech.sabai.contracteer.core.operation.ResponseSchema
import tech.sabai.contracteer.core.operation.Scenario

sealed class VerificationCase {
  abstract val displayName: String

  data class ScenarioBased(
    val scenario: Scenario,
    val requestSchema: RequestSchema,
    val responseSchema: ResponseSchema
  ) : VerificationCase() {
    override val displayName: String
      get() {
        val requestContentType = scenario.request.body?.contentType?.let { " (${it.value})" } ?: ""
        val responseContentType = scenario.response.body?.contentType?.let { " (${it.value})" } ?: ""
        return "${scenario.method.uppercase()} ${scenario.path}$requestContentType -> ${scenario.statusCode}$responseContentType with scenario '${scenario.key}'"
      }
  }

  data class SchemaBased(
    val path: String,
    val method: String,
    val statusCode: Int,
    val requestContentType: ContentType?,
    val responseContentType: ContentType?,
    val requestSchema: RequestSchema,
    val responseSchema: ResponseSchema
  ) : VerificationCase() {
    override val displayName: String
      get() {
        val requestCT = requestContentType?.let { " (${it.value})" } ?: ""
        val responseCT = responseContentType?.let { " (${it.value})" } ?: ""
        return "${method.uppercase()} $path$requestCT -> $statusCode$responseCT (generated)"
      }
  }
}
