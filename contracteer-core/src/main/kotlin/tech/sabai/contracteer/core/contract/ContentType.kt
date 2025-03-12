package tech.sabai.contracteer.core.contract

import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.datatype.ArrayDataType
import tech.sabai.contracteer.core.datatype.DataType

data class ContentType(val value: String) {

  fun validate(contentType: String) =
    when {
      value.trim() == "*/*"                                     -> success(value)
      contentType.trim().startsWith(value.substringBefore("*")) -> success(value)
      else                                                      -> failure("'Content-type' does not match: Expected: $value, actual: $contentType")
    }

  fun isJson() = "json" in value.lowercase()

  fun support(dataType: DataType<out Any>) =
    when {
      isJson() && !dataType.isFullyStructured() && dataType !is ArrayDataType -> failure("Content type $value supports only 'object', 'anyOf', 'oneOf', 'allOf' or 'array' schema")
      else -> success(dataType)
    }
}