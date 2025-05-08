package tech.sabai.contracteer.core.contract

import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.serde.BasicSerde
import tech.sabai.contracteer.core.serde.JsonSerde
import tech.sabai.contracteer.core.serde.Serde

data class ContentType(val value: String) {

  val serde: Serde = if (isJson()) JsonSerde else BasicSerde

  fun isJson() = "json" in value.lowercase()

  fun validate(contentType: String) =
    when {
      value.trim() == "*/*"                                     -> success(value)
      contentType.trim().startsWith(value.substringBefore("*")) -> success(value)
      else                                                      -> failure("'Content-type' does not match: Expected: $value, actual: $contentType")
    }
}
