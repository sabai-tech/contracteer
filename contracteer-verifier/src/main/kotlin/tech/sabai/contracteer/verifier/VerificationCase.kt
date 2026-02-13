package tech.sabai.contracteer.verifier

import tech.sabai.contracteer.core.operation.ContentType
import tech.sabai.contracteer.core.operation.ParameterElement.*
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

  data class TypeMismatchCase(
    val path: String,
    val method: String,
    val requestContentType: ContentType?,
    val responseContentType: ContentType?,
    val requestSchema: RequestSchema,
    val responseSchema: ResponseSchema,
    val mutatedElement: MutatedElement,
    val mutatedValue: String
  ) : VerificationCase() {
    override val displayName: String
      get() {
        val elementLabel = when (val element = mutatedElement) {
          is MutatedElement.Parameter -> {
            val category = when (element.element) {
              is PathParam  -> "path"
              is QueryParam -> "query"
              is Header     -> "header"
              is Cookie     -> "cookie"
            }
            "$category '${element.element.name}'"
          }
          is MutatedElement.Body      -> "body"
        }
        return "${method.uppercase()} $path -> 400 (auto: $elementLabel type mismatch)"
      }
  }
}
