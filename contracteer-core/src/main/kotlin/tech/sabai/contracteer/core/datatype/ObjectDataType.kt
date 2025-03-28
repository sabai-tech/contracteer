package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.accumulate
import tech.sabai.contracteer.core.joinWithQuotes

@Suppress("UNCHECKED_CAST")
class ObjectDataType private constructor(name: String,
                                         val properties: Map<String, DataType<out Any>>,
                                         val requiredProperties: Set<String> = emptySet(),
                                         val allowAdditionalProperties: Boolean,
                                         val additionalPropertiesDataType: DataType<out Any>?,
                                         isNullable: Boolean,
                                         allowedValues: AllowedValues? = null):
    DataType<Map<String, Any?>>(name,
                                "object",
                                isNullable,
                                Map::class.java as Class<Map<String, Any?>>,
                                allowedValues) {

  override fun isFullyStructured() = true

  override fun doValidate(value: Map<String, Any?>) =
    validateProperties(value) andThen { validateAdditionalProperties(value) }

  override fun doRandomValue() =
    properties.mapValues { it.value.randomValue() }

  private fun validateProperties(value: Map<String, Any?>): Result<Map<String, Any>> =
    properties.accumulate { (property, dataType) ->
      when {
        !value.containsKey(property) && !requiredProperties.contains(property) -> success(value)
        !value.containsKey(property)                                           -> failure(property, "is required")
        else                                                                   ->
          dataType.validate(value[property]).forProperty(property)
      }
    }

  private fun validateAdditionalProperties(value: Map<String, Any?>): Result<Map<String, Any?>> {
    val extraProperties = value.keys - properties.keys
    return when {
      extraProperties.isNotEmpty() && !allowAdditionalProperties -> failure("Additional properties are not allowed. Unexpected properties: " + extraProperties.joinWithQuotes())
      additionalPropertiesDataType == null                       -> success(value)
      else                                                       ->
        extraProperties.accumulate { additionalPropertiesDataType.validate(value[it]).forProperty(it) }.map { value }
    }
  }

  companion object {
    fun create(
      name: String,
      properties: Map<String, DataType<out Any>>,
      requiredProperties: Set<String> = emptySet(),
      allowAdditionalProperties: Boolean,
      additionalPropertiesDataType: DataType<out Any>? = null,
      isNullable: Boolean,
      enum: List<Any?> = emptyList()
    ): Result<ObjectDataType> {
      val undefinedProperties = requiredProperties - properties.keys
      if (undefinedProperties.isNotEmpty()) return failure("The following required properties are not defined in the schema: " + undefinedProperties.joinWithQuotes())
      val default = ObjectDataType(name,
                                   properties,
                                   requiredProperties,
                                   allowAdditionalProperties,
                                   additionalPropertiesDataType,
                                   isNullable)
      return if (enum.isEmpty())
        success(default)
      else
        AllowedValues
          .create(enum, default)
          .map {
            ObjectDataType( name,
              properties,
              requiredProperties,
              allowAdditionalProperties,
              additionalPropertiesDataType,
              isNullable,
              it)
          }
    }
  }
}
