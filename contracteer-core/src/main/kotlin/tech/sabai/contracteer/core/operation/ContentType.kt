package tech.sabai.contracteer.core.operation

import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.serde.JsonSerde
import tech.sabai.contracteer.core.serde.PlainTextSerde
import tech.sabai.contracteer.core.serde.Serde

/**
 * Represents a media type (e.g. `application/json`, `text/plain`).
 *
 * Resolves the appropriate [Serde] for serialization and deserialization based on the media type.
 */
data class ContentType(val value: String) {

  val serde: Serde = if (isJson()) JsonSerde else PlainTextSerde

  fun isJson() = "json" in value.lowercase()

  fun validate(contentType: String) =
    when {
      value.trim() == "*/*"                                     -> success(value)
      contentType.trim().startsWith(value.substringBefore("*")) -> success(value)
      else                                                      -> failure("'Content-type' does not match: Expected: $value, actual: $contentType")
    }
}
