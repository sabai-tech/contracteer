package tech.sabai.contracteer.core.codec

import tech.sabai.contracteer.core.serde.PlainTextSerde

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.datatype.ArrayDataType
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.datatype.ObjectDataType

/**
 * [StyleCodec] for OpenAPI `simple` style. Used for path and header parameters.
 *
 * Encoding:
 * - Primitive: `value`
 * - Array: `value1,value2,value3` (both explode values)
 * - Object explode=false: `key1,value1,key2,value2`
 * - Object explode=true: `key1=value1,key2=value2`
 */
data class SimpleStyleCodec(override val paramName: String, val explode: Boolean) : StyleCodec {

  override fun encode(value: Any?): List<Pair<String, String>> {
    val encoded = when (value) {
      is List<*>              -> serializeItems(value, ",")
      is Map<*, *> if explode -> serializeKeyValueEntries(value, ",")
      is Map<*, *>            -> serializeFlatEntries(value)
      else                    -> PlainTextSerde.serialize(value)
    }
    return listOf(paramName to encoded)
  }

  override fun decode(valueExtractor: (String) -> List<String>, dataType: DataType<out Any>): Result<Any?> {
    val values = valueExtractor(paramName)
    if (values.isEmpty()) return Result.success(null)
    val raw = values.first()

    return when (dataType) {
      is ArrayDataType             -> deserializeItems(raw.split(","), dataType.itemDataType)
      is ObjectDataType if explode -> deserializeKeyValuePairs(raw.split(","), dataType)
      is ObjectDataType            -> deserializeFlatEntries(raw.split(","), dataType)
      else                         -> PlainTextSerde.deserialize(raw, dataType)
    }
  }
}