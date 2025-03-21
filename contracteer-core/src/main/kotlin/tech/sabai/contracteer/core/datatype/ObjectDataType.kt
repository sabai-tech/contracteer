package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.accumulate

@Suppress("UNCHECKED_CAST")
class ObjectDataType private constructor(name: String,
                                         val properties: Map<String, DataType<out Any>>,
                                         val requiredProperties: Set<String> = emptySet(),
                                         isNullable: Boolean = false,
                                         allowedValues: AllowedValues? = null):
    DataType<Map<String, Any?>>(name,
                                "object",
                                isNullable,
                                Map::class.java as Class<Map<String, Any?>>,
                                allowedValues) {

  override fun isFullyStructured() = true

  override fun doValidate(value: Map<String, Any?>): Result<Map<String, Any?>> =
    properties.accumulate {
      when {
        !value.containsKey(it.key) && !isRequired(it.key) -> success(value)
        !value.containsKey(it.key)                        -> failure(it.key, "is required")
        else                                              -> it.value.validate(value[it.key]).forProperty(it.key)
      }
    }.map { value }

  private fun isRequired(key: String) =
    requiredProperties.contains(key)

  override fun doRandomValue() =
    properties.mapValues { it.value.randomValue() }

  companion object {
    fun create(
      name: String,
      properties: Map<String, DataType<out Any>>,
      requiredProperties: Set<String> = emptySet(),
      isNullable: Boolean = false,
      enum: List<Any?> = emptyList()
    ): Result<ObjectDataType> {
      val undefinedProperties = requiredProperties.filter { it !in properties.keys }

      return if (undefinedProperties.isNotEmpty()) {
        failure(
          "undefined required properties: " +
          undefinedProperties.joinToString("', '", "'", "'")
        )
      } else {
        ObjectDataType(name, properties, requiredProperties, isNullable).let { dataType ->
          if (enum.isEmpty()) success(dataType)
          else AllowedValues
            .create(enum, dataType)
            .map { ObjectDataType(name, properties, requiredProperties, isNullable, it) }
        }
      }
    }
  }
}