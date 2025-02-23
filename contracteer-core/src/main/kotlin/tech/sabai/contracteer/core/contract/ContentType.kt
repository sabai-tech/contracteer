package tech.sabai.contracteer.core.contract

import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success

data class ContentType(val value: String) {

  fun validate(contentType: String) =
    when {
      value.trim() == "*/*"                                     -> success(value)
      contentType.trim().startsWith(value.substringBefore("*")) -> success(value)
      else                                                      -> failure("'Content-type' does not match: Expected: $value, actual: $contentType")
    }

  fun isJson() = "json" in value.lowercase()

}