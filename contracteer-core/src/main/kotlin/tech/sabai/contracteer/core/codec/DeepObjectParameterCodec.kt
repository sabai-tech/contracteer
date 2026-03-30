package tech.sabai.contracteer.core.codec

import tech.sabai.contracteer.core.serde.PlainTextSerde

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.datatype.ObjectDataType

/**
 * [ParameterCodec] for OpenAPI `deepObject` style. Used for query parameters only.
 * Always has explode=true. Supports objects only, not arrays.
 *
 * Encoding: multiple entries `(name[key1], value1), (name[key2], value2), ...`
 */
data class DeepObjectParameterCodec(override val paramName: String) : ParameterCodec {
  val explode = true

  override fun encode(value: Any?): List<Pair<String, String>> = when (value) {
    is Map<*, *> -> value.entries.map { "$paramName[${it.key}]" to PlainTextSerde.serialize(it.value) }
    else -> encodePrimitive(paramName, value)
  }

  override fun decode(valueExtractor: (String) -> List<String>, dataType: DataType<out Any>): Result<Any?> =
    when (dataType) {
      is ObjectDataType -> {
        val extractor = { propName: String -> valueExtractor("$paramName[$propName]") }
        deserializeProperties(extractor, dataType)
      }
      else -> decodePrimitive(valueExtractor, paramName, dataType)
    }
}
