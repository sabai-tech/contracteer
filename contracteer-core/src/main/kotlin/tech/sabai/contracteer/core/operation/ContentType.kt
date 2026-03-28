package tech.sabai.contracteer.core.operation

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

  fun validate(contentType: String) =
    when {
      value.trim() == "*/*"                                     -> success(value)
      contentType.trim().startsWith(value.substringBefore("*")) -> success(value)
      else                                                      -> failure("'Content-type' does not match: Expected: $value, actual: $contentType")
    }
}
