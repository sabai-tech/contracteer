package dev.contracteer.core.codec

import dev.contracteer.core.Result
import dev.contracteer.core.Result.Companion.success
import dev.contracteer.core.datatype.DataType
import dev.contracteer.core.serde.PlainTextSerde

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
    if (rawValues.isEmpty()) return success(null)
    val raw = rawValues.first()

    return EncodingShape.of(dataType).flatMap { shape ->
      when (shape) {
        is EncodingShape.Array -> deserializeItems(raw.split(" "), shape.itemType)
        else                   -> PlainTextSerde.deserialize(raw, dataType)
      }
    }
  }
}
