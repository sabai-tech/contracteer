package tech.sabai.contracteer.core.codec

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.datatype.ArrayDataType
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.serde.PlainTextSerde

/**
 * [ParameterCodec] for OpenAPI `spaceDelimited` style. Used for query parameters only.
 * Always has explode=false. Supports arrays only.
 *
 * Encoding: single entry `(name, "value1 value2 value3")`
 *
 * URL encoding of spaces (%20) is handled by the HTTP framework, not by this codec.
 */
data class SpaceDelimitedParameterCodec(
  override val paramName: String,
  override val allowReserved: Boolean = false
): ParameterCodec {
  val explode = false

  override fun encode(value: Any?): List<Pair<String, String>> = when (value) {
    is List<*> -> listOf(paramName to serializeItems(value, " "))
    else       -> encodePrimitive(paramName, value)
  }

  override fun decode(values: Map<String, List<String>>, dataType: DataType<out Any>): Result<Any?> {
    val rawValues = values[paramName].orEmpty()
    return when {
      rawValues.isEmpty()       -> success(null)
      dataType is ArrayDataType -> deserializeItems(rawValues.first().split(" "), dataType.itemDataType)
      else                      -> PlainTextSerde.deserialize(rawValues.first(), dataType)
    }
  }
}
