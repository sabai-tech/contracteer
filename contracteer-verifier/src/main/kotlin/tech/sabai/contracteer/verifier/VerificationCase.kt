package tech.sabai.contracteer.verifier

import tech.sabai.contracteer.core.operation.ContentType
import tech.sabai.contracteer.core.operation.ParameterElement.*
import tech.sabai.contracteer.core.operation.RequestSchema
import tech.sabai.contracteer.core.operation.ResponseSchema
import tech.sabai.contracteer.core.operation.Scenario

/**
 * A test case for verifying a server implementation against an OpenAPI contract.
 *
 * Each subtype represents a different verification strategy:
 * - [ScenarioBased]: driven by a named scenario from the specification
 * - [SchemaBased]: generated from the schema when no 2xx scenario exists
 * - [TypeMismatch]: sends an intentionally malformed request to verify 400 handling
 */
sealed class VerificationCase {
  /** A human-readable description of this verification case, suitable for test output. */
  abstract val displayName: String

  /** A verification case driven by a named [Scenario] from the OpenAPI specification. */
  data class ScenarioBased(
    val scenario: Scenario,
    val requestSchema: RequestSchema,
    val responseSchema: ResponseSchema
  ): VerificationCase() {
    override val displayName: String
      get() {
        val requestContentType = scenario.request.body?.contentType?.let { " (${it.value})" } ?: ""
        val responseContentType = scenario.response.body?.contentType?.let { " (${it.value})" } ?: ""
        return "${scenario.method.uppercase()} ${scenario.path}$requestContentType -> ${scenario.statusCode}$responseContentType with scenario '${scenario.key}'"
      }
  }

  /** A verification case generated from the schema with random values, used when no 2xx scenario exists. */
  data class SchemaBased(
    val path: String,
    val method: String,
    val statusCode: Int,
    val requestContentType: ContentType?,
    val responseContentType: ContentType?,
    val requestSchema: RequestSchema,
    val responseSchema: ResponseSchema
  ): VerificationCase() {
    override val displayName: String
      get() {
        val requestCT = requestContentType?.let { " (${it.value})" } ?: ""
        val responseCT = responseContentType?.let { " (${it.value})" } ?: ""
        return "${method.uppercase()} $path$requestCT -> $statusCode$responseCT (generated)"
      }
  }

  /** A verification case that sends a type-mismatched value to verify 400 Bad Request handling. */
  data class TypeMismatch(
    val path: String,
    val method: String,
    val requestContentType: ContentType?,
    val responseContentType: ContentType?,
    val requestSchema: RequestSchema,
    val responseSchema: ResponseSchema,
    val mutatedElement: MutatedElement,
    val mutatedValue: String
  ): VerificationCase() {
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
