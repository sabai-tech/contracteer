package tech.sabai.contracteer.core.operation

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success

/**
 * Represents an HTTP content type (e.g. `application/json`, `text/plain`).
 *
 * Used for content type identification and validation. Serialization strategy
 * is determined by [BodySchema.serde], not by this class.
 */
data class ContentType(val value: String) {

  private val normalized = value.lowercase().substringBefore(";").trim()

  fun isJson() = "json" in normalized

  fun isFormUrlEncoded() = normalized == "application/x-www-form-urlencoded"

  fun isMultipart() = normalized.startsWith("multipart/")

  fun isXml() = "xml" in normalized

  fun isStructuredText() =
    !normalized.startsWith("text/") && STRUCTURED_TEXT_MARKERS.any { it in normalized }

  fun isBinary() =
    !normalized.startsWith("text/") &&
      !normalized.startsWith("multipart/") &&
      normalized != "application/x-www-form-urlencoded" &&
      !isStructuredText()

  /** Checks if [actual] matches this content type. This content type may use wildcards. */
  fun validate(actual: String): Result<String> {
    val received = actual.lowercase().substringBefore(";").trim()
    return when {
      normalized == "*/*"                                                                       -> success(actual)
      normalized.endsWith("/*") && received.startsWith(normalized.substringBefore("/*") + "/") -> success(actual)
      normalized == received                                                                    -> success(actual)
      else                                                                                      -> failure("'Content-type' does not match: Expected: $value, actual: $actual")
    }
  }

  companion object {
    // Substring heuristic. 
    // Upgrade to RFC 6839 structured-syntax-suffix parsing if a misclassified spec appears.
    private val STRUCTURED_TEXT_MARKERS = listOf("json", "xml", "yaml", "yml", "javascript")
  }
}