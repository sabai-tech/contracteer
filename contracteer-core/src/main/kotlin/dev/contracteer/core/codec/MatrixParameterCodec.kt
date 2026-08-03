package dev.contracteer.core.codec

import dev.contracteer.core.serde.PlainTextSerde

import dev.contracteer.core.Result
import dev.contracteer.core.Result.Companion.failure
import dev.contracteer.core.Result.Companion.success
import dev.contracteer.core.datatype.DataType

/**
 * [ParameterCodec] for OpenAPI `matrix` style. Used for path parameters only.
 *
 * Encoding:
 * - Primitive: `;name=value`
 * - Array explode=false: `;name=value1,value2,value3`
 * - Array explode=true: `;name=value1;name=value2;name=value3`
 * - Object explode=false: `;name=key1,value1,key2,value2`
 * - Object explode=true: `;key1=value1;key2=value2`
 */
data class MatrixParameterCodec(override val paramName: String, val explode: Boolean) : ParameterCodec {

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

  override fun decode(values: Map<String, List<String>>, dataType: DataType<out Any>): Result<Any?> {
    val rawValues = values[paramName].orEmpty()
    if (rawValues.isEmpty()) return success(null)
    val raw = rawValues.first()
    if (!raw.startsWith(";")) return failure("Matrix style value must start with ';'")

    return EncodingShape.of(dataType).flatMap { shape ->
      when (shape) {
        is EncodingShape.Array  if explode              -> decodeExplodedArrayItems(raw, shape.itemType)
        is EncodingShape.Array                          -> extractValue(raw) { content -> deserializeItems(content.split(","), shape.itemType) }
        is EncodingShape.Object if explode              -> deserializeKeyValuePairs(raw.split(";").filter { it.isNotEmpty() }, shape.properties)
        is EncodingShape.Object                         -> extractValue(raw) { content -> deserializeFlatEntries(content.split(","), shape.properties) }
        is EncodingShape.Scalar, is EncodingShape.Mixed -> extractValue(raw) { PlainTextSerde.deserialize(it, dataType) }
      }
    }
  }

  private fun decodeExplodedArrayItems(raw: String, itemView: DecodeView): Result<Any?> {
    val items = raw.split(";").filter { it.isNotEmpty() }.mapNotNull { segment ->
      val parts = segment.split("=", limit = 2)
      if (parts.size == 2 && parts[0] == paramName) parts[1] else null
    }
    return deserializeItems(items, itemView)
  }

  private fun extractValue(raw: String, parse: (String) -> Result<Any?>): Result<Any?> {
    val prefix = ";$paramName="
    if (!raw.startsWith(prefix)) return failure("Expected ';$paramName=' prefix")
    return parse(raw.substring(prefix.length))
  }
}
