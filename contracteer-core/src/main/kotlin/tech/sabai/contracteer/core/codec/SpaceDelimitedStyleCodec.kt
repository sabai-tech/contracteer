package tech.sabai.contracteer.core.codec

import tech.sabai.contracteer.core.serde.PlainTextSerde

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.datatype.ArrayDataType
import tech.sabai.contracteer.core.datatype.DataType

/**
 * [StyleCodec] for OpenAPI `spaceDelimited` style. Used for query parameters only.
 * Always has explode=false. Supports arrays only.
 *
 * Encoding: single entry `(name, "value1 value2 value3")`
 *
 * URL encoding of spaces (%20) is handled by the HTTP framework, not by this codec.
 */
data class SpaceDelimitedStyleCodec(override val paramName: String) : StyleCodec {
  val explode = false

  override fun encode(value: Any?): List<Pair<String, String>> = when (value) {
    is List<*> -> listOf(paramName to serializeItems(value, " "))
    else -> encodePrimitive(paramName, value)
  }

  override fun decode(valueExtractor: (String) -> List<String>, dataType: DataType<out Any>): Result<Any?> {
    val values = valueExtractor(paramName)
    if (values.isEmpty()) return Result.success(null)
    return when (dataType) {
      is ArrayDataType -> deserializeItems(values.first().split(" "), dataType.itemDataType)
      else -> PlainTextSerde.deserialize(values.first(), dataType)
    }
  }
}
