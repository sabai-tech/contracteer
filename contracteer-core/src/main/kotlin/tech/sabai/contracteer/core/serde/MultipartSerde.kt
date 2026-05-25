package tech.sabai.contracteer.core.serde

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.codec.DecodeView
import tech.sabai.contracteer.core.codec.EncodingShape
import tech.sabai.contracteer.core.codec.deserialize
import tech.sabai.contracteer.core.combineResults
import tech.sabai.contracteer.core.datatype.DataType

/**
 * [Serde] for multipart request bodies (form-data, mixed, etc.).
 *
 * Delegates to per-part [Serde]s for encoding/decoding individual parts.
 * Each part has its own content type and serde (e.g. [JsonSerde] for objects,
 * [PlainTextSerde] for primitives and binary).
 */
class MultipartSerde internal constructor(
  internal val partConfigs: Map<String, PartConfig>
): Serde() {

  val boundary: String = BOUNDARY

  override fun doSerialize(value: Any?): String {
    require(value is Map<*, *>) { "MultipartSerde expects a Map but received ${value?.let { it::class.simpleName }}" }

    return value.entries
             .filter { (key, _) -> key.toString() in partConfigs }
             .flatMap { (key, propValue) -> serializeProperty(key.toString(), propValue) }
             .joinToString("") + "--$boundary--\r\n"
  }

  override fun doDeserialize(source: String?, targetDataType: DataType<out Any>): Result<Any?> {
    if (source == null) return success(null)
    val parts = parseParts(source) ?: return failure("Invalid multipart body")

    return partConfigs.entries
      .filter { (propName, _) -> propName in parts }
      .map { (propName, config) -> deserializePart(propName, config, parts.getValue(propName)) }
      .combineResults()
      .map { pairs -> pairs.filter { it.second != null }.toMap() }
  }

  private fun serializeProperty(name: String, value: Any?): List<String> {
    val config = partConfigs.getValue(name)
    return if (config.expandArray && value is List<*>)
      value.map { serializePart(name, config, it) }
    else
      listOf(serializePart(name, config, value))
  }

  private fun deserializePart(propName: String,
                              config: PartConfig,
                              values: List<String>): Result<Pair<String, Any?>> =
    if (config.expandArray)
      config.view.shape().flatMap { shape ->
        when (shape) {
          is EncodingShape.Array -> values.map { config.serde.deserialize(it, shape.itemType) }.combineResults()
          else                    -> failure("Expected an array schema for expandArray part '$propName'")
        }
      }.map { propName to it }
    else
      config.serde.deserialize(values.first(), config.view).map { propName to it }

  private fun serializePart(name: String, config: PartConfig, value: Any?): String {
    val disposition =
      if (config.isFile) "form-data; name=\"$name\"; filename=\"$name\""
      else "form-data; name=\"$name\""

    return "--$boundary\r\n" +
           "Content-Disposition: $disposition\r\n" +
           "Content-Type: ${config.contentType}\r\n" +
           "\r\n" +
           "${config.serde.serialize(value)}\r\n"
  }

  private fun parseParts(source: String): Map<String, List<String>>? {
    val boundary = extractBoundary(source) ?: return null

    return source
      .split("--$boundary")
      .drop(1)
      .dropLast(1)
      .mapNotNull { parseSinglePart(it) }
      .groupBy({ it.first }, { it.second })
  }

  private fun extractBoundary(source: String): String? {
    val firstLine = source.lineSequence().firstOrNull() ?: return null
    return if (firstLine.startsWith("--")) firstLine.removePrefix("--").trimEnd('\r') else null
  }

  private fun parseSinglePart(part: String): Pair<String, String>? {
    val trimmed = part.removePrefix("\r\n")
    val headerBodySplit = trimmed.indexOf(HEADER_BODY_SEPARATOR)
    if (headerBodySplit == -1) return null
    val name = extractFieldName(trimmed.substring(0, headerBodySplit)) ?: return null
    val body = trimmed.substring(headerBodySplit + HEADER_BODY_SEPARATOR.length).removeSuffix("\r\n")
    return name to body
  }

  private fun extractFieldName(headers: String): String? =
    FIELD_NAME_REGEX.find(headers)?.groupValues?.get(1)

  companion object {
    const val BOUNDARY = "contracteer-boundary"
    private const val HEADER_BODY_SEPARATOR = "\r\n\r\n"
    private val FIELD_NAME_REGEX = Regex("""name="([^"]+)"""")
  }
}

internal data class PartConfig(
  val contentType: String,
  val serde: Serde,
  val view: DecodeView,
  val isFile: Boolean = false,
  val expandArray: Boolean = false
)