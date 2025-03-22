package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.accumulate
import tech.sabai.contracteer.core.joinWithQuotes
import kotlin.random.Random

@Suppress("UNCHECKED_CAST")
class MapDataType private constructor(name: String,
                                      val properties: Set<String>,
                                      val requiredProperties: Set<String> = emptySet(),
                                      val valueDataType: DataType<out Any>,
                                      isNullable: Boolean,
                                      allowedValues: AllowedValues? = null):
    DataType<Map<String, Any?>>(name,
                                "object",
                                isNullable,
                                Map::class.java as Class<Map<String, Any?>>,
                                allowedValues) {

  override fun isFullyStructured() = true

  override fun doValidate(value: Map<String, Any?>): Result<Map<String, Any?>> {
    val missingRequiredProperties = requiredProperties - value.keys
    return if (missingRequiredProperties.isNotEmpty())
      failure("missing required properties: ${missingRequiredProperties.joinWithQuotes()}")
    else
      value.accumulate { valueDataType.validate(it.value).forProperty(it.key) }.map { value }
  }

  override fun doRandomValue(): Map<String, Any> {
    val candidateChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    return (0..Random.nextInt(1000000)).associate {
      (1..Random.nextInt(10) + 1).map { candidateChars.random() }.joinToString("") to valueDataType.randomValue()
    }.plus(
      requiredProperties.associate { it to valueDataType.randomValue() }
    )
  }

  companion object {
    fun create(
      name: String,
      properties: Set<String> = emptySet(),
      requiredProperties: Set<String> = emptySet(),
      valueDataType: DataType<out Any>,
      isNullable: Boolean = false,
      enum: List<Any?> = emptyList()
    ): Result<MapDataType> {
      val undefinedProperties = requiredProperties - properties
      return if (undefinedProperties.isNotEmpty())
        failure("undefined required properties: " + undefinedProperties.joinWithQuotes())
      else
        MapDataType(name, properties, requiredProperties, valueDataType, isNullable)
          .let { dataType ->
            if (enum.isEmpty()) success(dataType)
            else AllowedValues
              .create(enum, dataType)
              .map { MapDataType(name, properties, requiredProperties, valueDataType, isNullable, it) }
          }
    }
  }
}