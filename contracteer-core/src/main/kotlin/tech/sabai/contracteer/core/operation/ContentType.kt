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

  fun isJson() = "json" in value.lowercase()

  fun isFormUrlEncoded() = value.lowercase() == "application/x-www-form-urlencoded"

  fun isMultipart() = value.lowercase().startsWith("multipart/")

  /** Checks if [actual] matches this content type. This content type may use wildcards. */
  fun validate(actual: String): Result<String> {
    val expected = value.lowercase().trim().substringBefore(";").trim()
    val received = actual.lowercase().trim().substringBefore(";").trim()
    return when {
      expected == "*/*"                                                                    -> success(actual)
      expected.endsWith("/*") && received.startsWith(expected.substringBefore("/*") + "/") -> success(actual)
      expected == received                                                                 -> success(actual)
      else                                                                                 -> failure("'Content-type' does not match: Expected: $value, actual: $actual")
    }
  }
}