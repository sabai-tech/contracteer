package tech.sabai.contracteer.core.codec

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.combineResults
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.datatype.ObjectDataType
import tech.sabai.contracteer.core.serde.PlainTextSerde

/**
 * [ParameterCodec] for OpenAPI `deepObject` style. Used for query parameters only.
 * Always has explode=true. Supports objects only, not arrays.
 *
 * Encoding: multiple entries `(name[key1], value1), (name[key2], value2), ...`
 */
data class DeepObjectParameterCodec(
  override val paramName: String,
  override val allowReserved: Boolean = false
): ParameterCodec {
  val explode = true

  override fun encode(value: Any?): List<Pair<String, String>> =
    when (value) {
      is Map<*, *> -> value.entries.map { "$paramName[${it.key}]" to PlainTextSerde.serialize(it.value) }
      else         -> encodePrimitive(paramName, value)
    }

  override fun decode(values: Map<String, List<String>>, dataType: DataType<out Any>): Result<Any?> =
    EncodingShape.of(dataType).flatMap { shape ->
      require(shape is EncodingShape.Object) { "DeepObjectParameterCodec requires object-like data type" }
      decodeObject(extractPropertyValues(values), shape.properties, shape.additionalProperties)
    }

  override fun supportsTypeMismatchMutation(dataType: DataType<out Any>): Boolean =
    !(dataType is ObjectDataType && dataType.isStructurallyOpen())

  private fun extractPropertyValues(values: Map<String, List<String>>): Map<String, List<String>> {
    val prefix = "$paramName["
    return values
      .filterKeys { it.startsWith(prefix) && it.endsWith("]") }
      .mapKeys { (key, _) -> key.substring(prefix.length, key.length - 1) }
  }

  private fun decodeObject(propertyValues: Map<String, List<String>>,
                           properties: Map<String, DecodeView>,
                           policy: EncodingShape.AdditionalPropertiesPolicy): Result<Any?> {
    val declaredEntries = decodeDeclaredProperties(propertyValues, properties)
    val additionalEntries =
      if (policy.mustDecode()) decodeAdditionalProperties(propertyValues, properties.keys, policy.itemType)
      else emptyList()
    return (declaredEntries + additionalEntries).combineResults().map { entries ->
      if (entries.isEmpty()) null else entries.toMap()
    }
  }

  private fun decodeDeclaredProperties(propertyValues: Map<String, List<String>>,
                                       properties: Map<String, DecodeView>): List<Result<Pair<String, Any?>>> =
    properties.mapNotNull { (name, propView) ->
      propertyValues[name]?.firstOrNull()?.let { raw ->
        PlainTextSerde.deserialize(raw, propView).map { name to it }
      }
    }

  private fun decodeAdditionalProperties(propertyValues: Map<String, List<String>>,
                                         declaredNames: Set<String>,
                                         additionalPropertiesView: DecodeView?): List<Result<Pair<String, Any?>>> {
    val additionalNames = propertyValues.keys - declaredNames
    return additionalNames.mapNotNull { name ->
      propertyValues[name]?.firstOrNull()?.let { raw ->
        if (additionalPropertiesView != null)
          PlainTextSerde.deserialize(raw, additionalPropertiesView).map { name to it }
        else
          success<Pair<String, Any?>>(name to raw)
      }
    }
  }

  private fun EncodingShape.AdditionalPropertiesPolicy.mustDecode(): Boolean = !allowed || itemType != null
}
