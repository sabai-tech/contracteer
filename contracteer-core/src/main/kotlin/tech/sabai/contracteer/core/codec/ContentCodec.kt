package tech.sabai.contracteer.core.codec

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.serde.Serde

/**
 * [ParameterCodec] for parameters defined with the `content` keyword.
 *
 * Instead of using OAS style/explode rules, the parameter value is serialized
 * and deserialized using a [Serde] (e.g., JSON-encoded query parameter).
 */
data class ContentCodec(override val paramName: String, val serde: Serde) : ParameterCodec {

  override fun encode(value: Any?): List<Pair<String, String>> =
    listOf(paramName to serde.serialize(value))

  override fun decode(valueExtractor: (String) -> List<String>, dataType: DataType<out Any>): Result<Any?> {
    val values = valueExtractor(paramName)
    if (values.isEmpty()) return success(null)
    return serde.deserialize(values.first(), dataType)
  }
}