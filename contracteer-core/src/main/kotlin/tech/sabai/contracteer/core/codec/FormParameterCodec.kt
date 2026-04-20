package tech.sabai.contracteer.core.codec

import tech.sabai.contracteer.core.serde.PlainTextSerde

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.datatype.ArrayDataType
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.datatype.ObjectDataType

/**
 * [ParameterCodec] for OpenAPI `form` style. Used for query and cookie parameters.
 *
 * Encoding:
 * - Primitive: single entry `(name, value)`
 * - Array explode=false: single entry `(name, "value1,value2,value3")`
 * - Array explode=true: multiple entries `(name, value1), (name, value2), ...`
 * - Object explode=false: single entry `(name, "key1,value1,key2,value2")`
 * - Object explode=true: multiple entries `(key1, value1), (key2, value2), ...`
 */
data class FormParameterCodec(
  override val paramName: String,
  val explode: Boolean,
  override val allowReserved: Boolean = false
) : ParameterCodec {

  override fun encode(value: Any?): List<Pair<String, String>> = when {
    value is List<*> && explode  -> value.map { paramName to PlainTextSerde.serialize(it) }
    value is List<*>             -> listOf(paramName to serializeItems(value, ","))
    value is Map<*, *> && explode -> value.entries.map { it.key.toString() to PlainTextSerde.serialize(it.value) }
    value is Map<*, *>            -> listOf(paramName to serializeFlatEntries(value))
    else                          -> encodePrimitive(paramName, value)
  }

  override fun decode(valueExtractor: (String) -> List<String>, dataType: DataType<out Any>): Result<Any?> =
    when (dataType) {
      is ArrayDataType if explode  -> decodeMultiValueItems(valueExtractor, dataType)
      is ArrayDataType             -> decodeSingleValue(valueExtractor) { deserializeItems(it.split(","), dataType.itemDataType) }
      is ObjectDataType if explode -> deserializeProperties(valueExtractor, dataType)
      is ObjectDataType            -> decodeSingleValue(valueExtractor) { deserializeFlatEntries(it.split(","), dataType) }
      else                         -> decodePrimitive(valueExtractor, paramName, dataType)
    }

  private fun decodeMultiValueItems(valueExtractor: (String) -> List<String>, dataType: ArrayDataType): Result<Any?> {
    val allValues = valueExtractor(paramName)
    return if (allValues.isEmpty()) Result.success(null)
    else deserializeItems(allValues, dataType.itemDataType)
  }

  private fun decodeSingleValue(valueExtractor: (String) -> List<String>, parse: (String) -> Result<Any?>): Result<Any?> {
    val values = valueExtractor(paramName)
    return if (values.isEmpty()) Result.success(null)
    else parse(values.first())
  }
}
