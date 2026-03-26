package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.accumulate
import tech.sabai.contracteer.core.joinWithQuotes

/** OpenAPI `object` type, with named properties, required property constraints, and optional additional properties. */
@Suppress("UNCHECKED_CAST")
class ObjectDataType private constructor(name: String,
                                         val properties: Map<String, DataType<out Any>>,
                                         val requiredProperties: Set<String> = emptySet(),
                                         val readOnlyProperties: Set<String> = emptySet(),
                                         val writeOnlyProperties: Set<String> = emptySet(),
                                         val allowAdditionalProperties: Boolean,
                                         val additionalPropertiesDataType: DataType<out Any>?,
                                         isNullable: Boolean,
                                         val minProperties: Int?,
                                         val maxProperties: Int?,
                                         allowedValues: AllowedValues? = null):
    DataType<Map<String, Any?>>(name,
                                "object",
                                isNullable,
                                Map::class.java as Class<Map<String, Any?>>,
                                allowedValues) {

  override fun isFullyStructured() = true

  override fun doValidate(value: Map<String, Any?>): Result<Map<String, Any?>> =
    validatePropertyCount(value) andThen
      { validateProperties(value) } andThen
      { validateAdditionalProperties(value) }

  private fun validatePropertyCount(value: Map<String, Any?>): Result<Map<String, Any?>> = when {
    minProperties != null && value.size < minProperties -> failure("Object has ${value.size} properties but minProperties is $minProperties")
    maxProperties != null && value.size > maxProperties -> failure("Object has ${value.size} properties but maxProperties is $maxProperties")
    else                                                -> success(value)
  }

  override fun doRandomValue(): Map<String, Any?> {
    val selected = if (maxProperties != null && maxProperties < properties.size) {
      val required = properties.filterKeys { it in requiredProperties }
      val optional = properties.filterKeys { it !in requiredProperties }
        .entries.shuffled()
        .take(maxProperties - required.size)
        .associate { it.key to it.value }
      required + optional
    } else {
      properties
    }
    return selected.mapValues { it.value.randomValue() }
  }

  override fun asRequestType(): DataType<Map<String, Any?>> {
    val transformedProperties = properties.minus(readOnlyProperties).mapValues { (_, v) -> v.asRequestType() }
    return if (readOnlyProperties.isEmpty() && transformedProperties.all { (k, v) -> v === properties[k] }) this
    else ObjectDataType(name = name,
                        properties = transformedProperties,
                        requiredProperties = requiredProperties - readOnlyProperties,
                        allowAdditionalProperties = allowAdditionalProperties,
                        additionalPropertiesDataType = additionalPropertiesDataType,
                        isNullable = isNullable,
                        minProperties = minProperties,
                        maxProperties = maxProperties,
                        allowedValues = allowedValues)
  }

  override fun asResponseType(): DataType<Map<String, Any?>> {
    val transformedProperties = properties.minus(writeOnlyProperties).mapValues { (_, v) -> v.asResponseType() }
    return if (writeOnlyProperties.isEmpty() && transformedProperties.all { (k, v) -> v === properties[k] }) this
    else ObjectDataType(name = name,
                        properties = transformedProperties,
                        requiredProperties = requiredProperties - writeOnlyProperties,
                        allowAdditionalProperties = allowAdditionalProperties,
                        additionalPropertiesDataType = additionalPropertiesDataType,
                        isNullable = isNullable,
                        minProperties = minProperties,
                        maxProperties = maxProperties,
                        allowedValues = allowedValues)
  }

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
    @JvmStatic
    @JvmOverloads
    fun create(
      name: String,
      properties: Map<String, DataType<out Any>>,
      requiredProperties: Set<String> = emptySet(),
      readOnlyProperties: Set<String> = emptySet(),
      writeOnlyProperties: Set<String> = emptySet(),
      allowAdditionalProperties: Boolean,
      additionalPropertiesDataType: DataType<out Any>? = null,
      isNullable: Boolean,
      enum: List<Any?> = emptyList(),
      minProperties: Int? = null,
      maxProperties: Int? = null
    ): Result<ObjectDataType> {
      val undefinedProperties = requiredProperties - properties.keys

      if (undefinedProperties.isNotEmpty())
        return failure("The following required properties are not defined in the schema: " + undefinedProperties.joinWithQuotes())
      if (minProperties != null && minProperties < 0)
        return failure("minProperties must be non-negative")
      if (maxProperties != null && maxProperties < 0)
        return failure("maxProperties must be non-negative")
      if (minProperties != null && maxProperties != null && minProperties > maxProperties)
        return failure("minProperties ($minProperties) must be less than or equal to maxProperties ($maxProperties)")
      if (maxProperties != null && maxProperties < requiredProperties.size)
        return failure("maxProperties ($maxProperties) is less than the number of required properties (${requiredProperties.size})")
      if (minProperties != null && minProperties > properties.size)
        return failure("minProperties ($minProperties) exceeds the number of declared properties (${properties.size})")
      if ((minProperties != null || maxProperties != null) && readOnlyProperties.isNotEmpty())
        return failure("minProperties/maxProperties cannot be combined with readOnly properties")
      if ((minProperties != null || maxProperties != null) && writeOnlyProperties.isNotEmpty())
        return failure("minProperties/maxProperties cannot be combined with writeOnly properties")

      val default = ObjectDataType(
        name = name,
        properties = properties,
        requiredProperties = requiredProperties,
        readOnlyProperties = readOnlyProperties,
        writeOnlyProperties = writeOnlyProperties,
        allowAdditionalProperties = allowAdditionalProperties,
        additionalPropertiesDataType = additionalPropertiesDataType,
        isNullable = isNullable,
        minProperties = minProperties,
        maxProperties = maxProperties
      )
      return if (enum.isEmpty())
        success(default)
      else
        AllowedValues
          .create(enum, default)
          .map {
            ObjectDataType(name = name,
                           properties = properties,
                           requiredProperties = requiredProperties,
                           readOnlyProperties = readOnlyProperties,
                           writeOnlyProperties = writeOnlyProperties,
                           allowAdditionalProperties = allowAdditionalProperties,
                           additionalPropertiesDataType = additionalPropertiesDataType,
                           isNullable = isNullable,
                           minProperties = minProperties,
                           maxProperties = maxProperties,
                           allowedValues = it)
          }
    }
  }
}
