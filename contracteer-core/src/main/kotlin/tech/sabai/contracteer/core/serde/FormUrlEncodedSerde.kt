package tech.sabai.contracteer.core.serde

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.codec.StyleCodec
import tech.sabai.contracteer.core.combineResults
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.datatype.ObjectDataType
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * [Serde] for `application/x-www-form-urlencoded` request bodies.
 *
 * Delegates to per-property [StyleCodec]s for encoding/decoding individual properties
 * of the object.
 */
class FormUrlEncodedSerde(
  internal val propertyCodecs: Map<String, StyleCodec>
): Serde() {

  override fun doSerialize(value: Any?): String {
    require(value is Map<*, *>) { "FormUrlEncodedSerde expects a Map but received ${value?.let { it::class.simpleName }}" }
    return value.entries
      .flatMap { (key, propValue) -> propertyCodecs.getValue(key.toString()).encode(propValue) }
      .joinToString("&") { (key, propValue) -> "${urlEncode(key)}=${urlEncode(propValue)}" }
  }

  override fun doDeserialize(source: String?, targetDataType: DataType<out Any>): Result<Any?> {
    if (source == null) return success(null)
    if (targetDataType !is ObjectDataType) return Result.failure("Form-urlencoded requires object type")

    val valueExtractor = buildValueExtractor(source)
    return propertyCodecs.entries
      .map { (propName, codec) ->
        codec
          .decode(valueExtractor, targetDataType.properties.getValue(propName))
          .map { propName to it }
      }
      .combineResults()
      .map { pairs -> pairs!!.filter { it.second != null }.toMap() }
  }

  private fun buildValueExtractor(source: String): (String) -> List<String> {
    val entries = source
      .split("&")
      .mapNotNull { entry ->
        val parts = entry.split("=", limit = 2)
        if (parts.size == 2) urlDecode(parts[0]) to urlDecode(parts[1]) else null
      }
    return { key -> entries.filter { it.first == key }.map { it.second } }
  }

  private fun urlEncode(value: String): String = URLEncoder.encode(value, "UTF-8")

  private fun urlDecode(value: String): String = URLDecoder.decode(value, "UTF-8")
}