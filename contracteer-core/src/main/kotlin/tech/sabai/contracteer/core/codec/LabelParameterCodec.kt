package tech.sabai.contracteer.core.codec

import tech.sabai.contracteer.core.serde.PlainTextSerde

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.datatype.ArrayDataType
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.datatype.ObjectDataType

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

  override fun decode(valueExtractor: (String) -> List<String>, dataType: DataType<out Any>): Result<Any?> {
    val values = valueExtractor(paramName)
    if (values.isEmpty()) return Result.success(null)

    val raw = values.first()
    if (!raw.startsWith(".")) return failure("Label style value must start with '.'")

    val content = raw.substring(1)
    return when (dataType) {
      is ArrayDataType if explode  -> deserializeItems(content.split("."), dataType.itemDataType)
      is ArrayDataType             -> deserializeItems(content.split(","), dataType.itemDataType)
      is ObjectDataType if explode -> deserializeKeyValuePairs(content.split("."), dataType)
      is ObjectDataType            -> deserializeFlatEntries(content.split(","), dataType)
      else                         -> PlainTextSerde.deserialize(content, dataType)
    }
  }
}
