package tech.sabai.contracteer.core.codec

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.combineResults
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.datatype.ObjectDataType
import tech.sabai.contracteer.core.serde.PlainTextSerde
import tech.sabai.contracteer.core.serde.Serde

/**
 * Encodes and decodes parameter values for HTTP transport.
 *
 * Implementations cover two OAS 3.0 serialization strategies:
 * - **Style-based**: `simple`, `form`, `label`, `matrix`, `pipeDelimited`,
 *   `spaceDelimited`, `deepObject` — encode via style/explode rules.
 * - **Content-based**: the parameter value is serialized using a media type
 *   (e.g., JSON-encoded query parameter via the `content` keyword).
 *
 * Unlike [Serde] (which serializes content bodies as single strings), `ParameterCodec`
 * produces a list of key-value pairs because some style/explode combinations expand
 * a single parameter into multiple HTTP-level entries.
 */
sealed interface ParameterCodec {

  /** The parameter name as declared in the OpenAPI specification. */
  val paramName: String

  /**
   * Encodes a typed value into key-value pairs for HTTP transport.
   *
   * Single-value styles produce one pair. Multi-value styles (e.g. `form` with
   * `explode=true`) may produce multiple pairs with the same or different keys.
   */
  fun encode(value: Any?): List<Pair<String, String>>

  /**
   * Decodes a typed value from HTTP transport using a value extractor.
   *
   * The [valueExtractor] returns all string values for a given key from an HTTP request.
   * The codec calls it with the appropriate key(s) depending on the style — once for
   * single-value styles, or per-property for exploded object styles.
   */
  fun decode(valueExtractor: (String) -> List<String>, dataType: DataType<out Any>): Result<Any?>
}

// === Shared encode helpers ===

internal fun encodePrimitive(paramName: String, value: Any?): List<Pair<String, String>> =
  listOf(paramName to PlainTextSerde.serialize(value))

internal fun serializeItems(items: List<*>, separator: String): String =
  items.joinToString(separator) { PlainTextSerde.serialize(it) }

internal fun serializeFlatEntries(entries: Map<*, *>): String =
  entries.flatMap { (k, v) -> listOf(k.toString(), PlainTextSerde.serialize(v)) }.joinToString(",")

internal fun serializeKeyValueEntries(entries: Map<*, *>, separator: String): String =
  entries.entries.joinToString(separator) { "${it.key}=${PlainTextSerde.serialize(it.value)}" }

// === Shared decode helpers ===

internal fun decodePrimitive(valueExtractor: (String) -> List<String>,
                             paramName: String,
                             dataType: DataType<out Any>): Result<Any?> {

  val values = valueExtractor(paramName)
  return if (values.isEmpty())
    success(null)
  else
    PlainTextSerde.deserialize(values.first(), dataType)
}

internal fun deserializeItems(items: List<String>, itemDataType: DataType<out Any>): Result<Any?> =
  items.map { PlainTextSerde.deserialize(it, itemDataType) }.combineResults()

internal fun deserializeFlatEntries(parts: List<String>, objectDataType: ObjectDataType): Result<Any?> {
  if (parts.size % 2 != 0) return failure("Invalid format: odd number of elements for object")
  return parts
    .chunked(2)
    .fold(success(emptyMap<String, Any?>())) { acc, (key, raw) ->
      acc.flatMap { map -> deserializeProperty(objectDataType, key, raw).map { map + (key to it) } }
    }
}

internal fun deserializeKeyValuePairs(pairs: List<String>, objectDataType: ObjectDataType): Result<Any?> =
  pairs.fold(success(emptyMap<String, Any?>())) { acc, pair ->
    acc.flatMap { map ->
      parseKeyValue(pair).flatMap { (key, raw) ->
        deserializeProperty(objectDataType, key, raw).map { map + (key to it) }
      }
    }
  }

internal fun deserializeProperties(valueExtractor: (String) -> List<String>,
                                   objectDataType: ObjectDataType): Result<Any?> =
  objectDataType.properties
    .mapNotNull { (name, dataType) ->
      valueExtractor(name).firstOrNull()?.let { name to PlainTextSerde.deserialize(it, dataType) }
    }
    .fold(success(emptyMap<String, Any?>())) { acc, (name, result) ->
      acc.flatMap { map -> result.map { map + (name to it) } }
    }
    .map { it.ifEmpty { null } }

private fun deserializeProperty(objectDataType: ObjectDataType, key: String, raw: String): Result<Any?> {
  val propDataType = objectDataType.properties[key] ?: return failure("Unknown property: $key")
  return PlainTextSerde.deserialize(raw, propDataType)
}

private fun parseKeyValue(pair: String): Result<Pair<String, String>> {
  val kv = pair.split("=", limit = 2)
  return if (kv.size == 2) success(kv[0] to kv[1]) else failure("Invalid key=value pair: $pair")
}
