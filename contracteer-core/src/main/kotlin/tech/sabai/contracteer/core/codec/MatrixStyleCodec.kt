package tech.sabai.contracteer.core.codec

import tech.sabai.contracteer.core.serde.PlainTextSerde

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.datatype.ArrayDataType
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.datatype.ObjectDataType

/**
 * [StyleCodec] for OpenAPI `matrix` style. Used for path parameters only.
 *
 * Encoding:
 * - Primitive: `;name=value`
 * - Array explode=false: `;name=value1,value2,value3`
 * - Array explode=true: `;name=value1;name=value2;name=value3`
 * - Object explode=false: `;name=key1,value1,key2,value2`
 * - Object explode=true: `;key1=value1;key2=value2`
 */
data class MatrixStyleCodec(override val paramName: String, val explode: Boolean) : StyleCodec {

  override fun encode(value: Any?): List<Pair<String, String>> {
    val encoded = when (value) {
      is List<*> if explode   -> value.joinToString("") { ";$paramName=${PlainTextSerde.serialize(it)}" }
      is List<*>              -> ";$paramName=${serializeItems(value, ",")}"
      is Map<*, *> if explode -> value.entries.joinToString("") { ";${it.key}=${PlainTextSerde.serialize(it.value)}" }
      is Map<*, *>            -> ";$paramName=${serializeFlatEntries(value)}"
      else                    -> ";$paramName=${PlainTextSerde.serialize(value)}"
    }
    return listOf(paramName to encoded)
  }

  override fun decode(valueExtractor: (String) -> List<String>, dataType: DataType<out Any>): Result<Any?> {
    val values = valueExtractor(paramName)
    if (values.isEmpty()) return Result.success(null)
    val raw = values.first()
    if (!raw.startsWith(";")) return failure("Matrix style value must start with ';'")

    return when (dataType) {
      is ArrayDataType if explode  -> decodeExplodedArrayItems(raw, dataType)
      is ArrayDataType             -> extractValue(raw) { deserializeItems(it.split(","), dataType.itemDataType) }
      is ObjectDataType if explode -> deserializeKeyValuePairs(raw.split(";").filter { it.isNotEmpty() }, dataType)
      is ObjectDataType            -> extractValue(raw) { deserializeFlatEntries(it.split(","), dataType) }
      else                         -> extractValue(raw) { PlainTextSerde.deserialize(it, dataType) }
    }
  }

  private fun decodeExplodedArrayItems(raw: String, dataType: ArrayDataType): Result<Any?> {
    val items = raw.split(";").filter { it.isNotEmpty() }.mapNotNull { segment ->
      val parts = segment.split("=", limit = 2)
      if (parts.size == 2 && parts[0] == paramName) parts[1] else null
    }
    return deserializeItems(items, dataType.itemDataType)
  }

  private fun extractValue(raw: String, parse: (String) -> Result<Any?>): Result<Any?> {
    val prefix = ";$paramName="
    if (!raw.startsWith(prefix)) return failure("Expected ';$paramName=' prefix")
    return parse(raw.substring(prefix.length))
  }
}
