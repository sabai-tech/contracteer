package tech.sabai.contracteer.core.codec

import tech.sabai.contracteer.core.serde.PlainTextSerde

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.success
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

  override fun encode(value: Any?): List<Pair<String, String>> =
    when (value) {
      is List<*> if explode   -> value.map { paramName to PlainTextSerde.serialize(it) }
      is List<*>              -> listOf(paramName to serializeItems(value, ","))
      is Map<*, *> if explode -> value.entries.map { it.key.toString() to PlainTextSerde.serialize(it.value) }
      is Map<*, *>            -> listOf(paramName to serializeFlatEntries(value))
      else                    -> encodePrimitive(paramName, value)
    }

  override fun decode(values: Map<String, List<String>>, dataType: DataType<out Any>): Result<Any?> =
    EncodingShape.of(dataType).flatMap { shape ->
      when (shape) {
        is EncodingShape.Array  if explode              -> decodeMultiValueItems(values, shape.itemType)
        is EncodingShape.Array                          -> decodeSingleValue(values) { raw -> deserializeItems(raw.split(","), shape.itemType) }
        is EncodingShape.Object if explode              -> deserializeProperties(values, shape.properties)
        is EncodingShape.Object                         -> decodeSingleValue(values) { raw -> deserializeFlatEntries(raw.split(","), shape.properties) }
        is EncodingShape.Scalar, is EncodingShape.Mixed -> decodePrimitive(values, paramName, dataType)
      }
    }

  override fun supportsTypeMismatchMutation(dataType: DataType<out Any>): Boolean =
    !(explode && dataType is ObjectDataType && dataType.isStructurallyOpen())

  private fun decodeMultiValueItems(values: Map<String, List<String>>, itemView: DecodeView): Result<Any?> {
    val allValues = values[paramName].orEmpty()
    return if (allValues.isEmpty()) success(null)
    else deserializeItems(allValues, itemView)
  }

  private fun decodeSingleValue(values: Map<String, List<String>>, parse: (String) -> Result<Any?>): Result<Any?> {
    val rawValues = values[paramName].orEmpty()
    return if (rawValues.isEmpty()) success(null)
    else parse(rawValues.first())
  }
}
