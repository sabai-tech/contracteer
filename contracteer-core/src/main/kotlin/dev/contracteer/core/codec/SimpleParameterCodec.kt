package dev.contracteer.core.codec

import dev.contracteer.core.Result
import dev.contracteer.core.Result.Companion.success
import dev.contracteer.core.datatype.DataType
import dev.contracteer.core.serde.PlainTextSerde

/**
 * [ParameterCodec] for OpenAPI `simple` style. Used for path and header parameters.
 *
 * Encoding:
 * - Primitive: `value`
 * - Array: `value1,value2,value3` (both explode values)
 * - Object explode=false: `key1,value1,key2,value2`
 * - Object explode=true: `key1=value1,key2=value2`
 */
data class SimpleParameterCodec(override val paramName: String, val explode: Boolean): ParameterCodec {

  override fun encode(value: Any?): List<Pair<String, String>> {
    val encoded = when (value) {
      is List<*>              -> serializeItems(value, ",")
      is Map<*, *> if explode -> serializeKeyValueEntries(value, ",")
      is Map<*, *>            -> serializeFlatEntries(value)
      else                    -> PlainTextSerde.serialize(value)
    }
    return listOf(paramName to encoded)
  }

  override fun decode(values: Map<String, List<String>>, dataType: DataType<out Any>): Result<Any?> {
    val rawValues = values[paramName].orEmpty()
    if (rawValues.isEmpty()) return success(null)
    val raw = rawValues.first()

    return EncodingShape.of(dataType).flatMap { shape ->
      when (shape) {
        is EncodingShape.Array                          -> deserializeItems(raw.split(","), shape.itemType)
        is EncodingShape.Object if explode              -> deserializeKeyValuePairs(raw.split(","), shape.properties)
        is EncodingShape.Object                         -> deserializeFlatEntries(raw.split(","), shape.properties)
        is EncodingShape.Scalar, is EncodingShape.Mixed -> PlainTextSerde.deserialize(raw, dataType)
      }
    }
  }
}