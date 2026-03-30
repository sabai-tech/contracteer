package tech.sabai.contracteer.core.codec

import tech.sabai.contracteer.core.serde.PlainTextSerde

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.datatype.ObjectDataType
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
  if (values.isEmpty()) return success(null)
  return PlainTextSerde.deserialize(values.first(), dataType)
}

internal fun deserializeItems(items: List<String>,
                              itemDataType: DataType<out Any>): Result<Any?> {
  val results = items.map { PlainTextSerde.deserialize(it, itemDataType) }
  val errors = results.filter { it.isFailure() }.flatMap { it.errors() }

  return if (errors.isNotEmpty()) failure(*errors.toTypedArray())
  else success(results.map { it.value })
}

internal fun deserializeFlatEntries(parts: List<String>,
                                    objectDataType: ObjectDataType): Result<Any?> {
  if (parts.size % 2 != 0) return failure("Invalid format: odd number of elements for object")
  val map = mutableMapOf<String, Any?>()
  for (i in parts.indices step 2) {
    val key = parts[i]
    val propDataType = objectDataType.properties[key]
                       ?: return failure("Unknown property: $key")
    val result = PlainTextSerde.deserialize(parts[i + 1], propDataType)
    if (result.isFailure()) return result.retypeError()
    map[key] = result.value
  }
  return success(map)
}

internal fun deserializeKeyValuePairs(pairs: List<String>,
                                      objectDataType: ObjectDataType): Result<Any?> {
  val map = mutableMapOf<String, Any?>()
  for (pair in pairs) {
    val kv = pair.split("=", limit = 2)
    if (kv.size != 2) return failure("Invalid key=value pair: $pair")
    val propDataType = objectDataType.properties[kv[0]]
                       ?: return failure("Unknown property: ${kv[0]}")
    val result = PlainTextSerde.deserialize(kv[1], propDataType)
    if (result.isFailure()) return result.retypeError()
    map[kv[0]] = result.value
  }
  return success(map)
}

internal fun deserializeProperties(valueExtractor: (String) -> List<String>,
                                   objectDataType: ObjectDataType): Result<Any?> {
  val map = mutableMapOf<String, Any?>()
  for ((propName, propDataType) in objectDataType.properties) {
    val values = valueExtractor(propName)
    if (values.isNotEmpty()) {
      val result = PlainTextSerde.deserialize(values.first(), propDataType)
      if (result.isFailure()) return result.retypeError()
      map[propName] = result.value
    }
  }
  return if (map.isEmpty()) success(null) else success(map)
}
