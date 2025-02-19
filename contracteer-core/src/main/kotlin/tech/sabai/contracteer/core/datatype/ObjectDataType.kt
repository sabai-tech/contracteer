package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.accumulate

class ObjectDataType private constructor(name: String,
                                         val properties: Map<String, DataType<*>>,
                                         val requiredProperties: Set<String>,
                                         isNullable: Boolean,
                                         allowedValues: AllowedValues? = null):
    StructuredObjectDataType(name, "object", isNullable, allowedValues) {

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
      name: String = "Inline 'object' Schema",
      properties: Map<String, DataType<*>>,
      requiredProperties: Set<String> = emptySet(),
      isNullable: Boolean = false,
      enum: List<Any?> = emptyList()
    ) =
      ObjectDataType(name, properties, requiredProperties, isNullable).let { dataType ->
        if (enum.isEmpty()) success(dataType)
        else AllowedValues.create(enum, dataType).map { ObjectDataType(name, properties, requiredProperties, isNullable, it) }
      }
  }
}