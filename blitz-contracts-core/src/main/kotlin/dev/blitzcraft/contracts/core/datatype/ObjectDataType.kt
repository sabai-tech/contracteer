package dev.blitzcraft.contracts.core.datatype

import dev.blitzcraft.contracts.core.validation.ValidationResult.Companion.success
import dev.blitzcraft.contracts.core.validation.ValidationResult.Companion.error
import dev.blitzcraft.contracts.core.validation.validate

class ObjectDataType(val properties: Map<String, DataType<*>>,
                     private val requiredProperties: Set<String> = emptySet(),
                     isNullable: Boolean = false):
    DataType<Map<*, *>>("object", isNullable = isNullable, Map::class.java) {

  override fun doValidate(value: Map<*, *>) =
    properties.validate {
      when {
        !value.containsKey(it.key) && !isRequired(it.key) -> success()
        !value.containsKey(it.key)                         -> error(it.key, "is required")
        else                                               -> it.value.validate(value[it.key]).forProperty(it.key)
      }
    }

  private fun isRequired(key: String) = requiredProperties.contains(key)

  override fun randomValue() =
    properties.mapValues { it.value.randomValue() }
}