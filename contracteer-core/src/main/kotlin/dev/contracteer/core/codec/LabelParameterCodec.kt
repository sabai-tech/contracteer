package dev.contracteer.core.codec

import dev.contracteer.core.serde.PlainTextSerde

import dev.contracteer.core.Result
import dev.contracteer.core.Result.Companion.failure
import dev.contracteer.core.Result.Companion.success
import dev.contracteer.core.datatype.DataType

/**
 * [ParameterCodec] for OpenAPI `label` style. Used for path parameters only.
 *
 * Encoding:
 * - Primitive: `.value`
 * - Array explode=false: `.value1,value2,value3`
 * - Array explode=true: `.value1.value2.value3`
 * - Object explode=false: `.key1,value1,key2,value2`
 * - Object explode=true: `.key1=value1.key2=value2`
 */
data class LabelParameterCodec(override val paramName: String, val explode: Boolean) : ParameterCodec {

  override fun encode(value: Any?): List<Pair<String, String>> {
    val encoded = when (value) {
      is List<*> if explode   -> ".${serializeItems(value, ".")}"
      is List<*>              -> ".${serializeItems(value, ",")}"
      is Map<*, *> if explode -> ".${serializeKeyValueEntries(value, ".")}"
      is Map<*, *>            -> ".${serializeFlatEntries(value)}"
      else                    -> ".${PlainTextSerde.serialize(value)}"
    }
    return listOf(paramName to encoded)
  }

  override fun decode(values: Map<String, List<String>>, dataType: DataType<out Any>): Result<Any?> {
    val rawValues = values[paramName].orEmpty()
    if (rawValues.isEmpty()) return success(null)

    val raw = rawValues.first()
    if (!raw.startsWith(".")) return failure("Label style value must start with '.'")

    val content = raw.substring(1)
    return EncodingShape.of(dataType).flatMap { shape ->
      when (shape) {
        is EncodingShape.Array  if explode              -> deserializeItems(content.split("."), shape.itemType)
        is EncodingShape.Array                          -> deserializeItems(content.split(","), shape.itemType)
        is EncodingShape.Object if explode              -> deserializeKeyValuePairs(content.split("."), shape.properties)
        is EncodingShape.Object                         -> deserializeFlatEntries(content.split(","), shape.properties)
        is EncodingShape.Scalar, is EncodingShape.Mixed -> PlainTextSerde.deserialize(content, dataType)
      }
    }
  }
}
