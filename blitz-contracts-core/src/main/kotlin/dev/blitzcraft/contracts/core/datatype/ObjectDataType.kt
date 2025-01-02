package dev.blitzcraft.contracts.core.datatype

import dev.blitzcraft.contracts.core.validation.ValidationResult.Companion.success
import dev.blitzcraft.contracts.core.validation.ValidationResult.Companion.error
import dev.blitzcraft.contracts.core.validation.validateEach

class ObjectDataType(name: String= "Inline Schema", val properties: Map<String, DataType<*>>,
                     val requiredProperties: Set<String> = emptySet(),
                     isNullable: Boolean = false):
    StructuredObjectDataType(name, "object", isNullable) {

  override fun doValidate(value: Map<String, Any?>) =
    properties.validateEach {
      when {
        !value.containsKey(it.key) && !isRequired(it.key) -> success()
        !value.containsKey(it.key)                        -> error(it.key, "is required")
        else                                              -> it.value.validate(value[it.key]).forProperty(it.key)
      }
    }

  private fun isRequired(key: String) = requiredProperties.contains(key)

  override fun randomValue() =
    properties.mapValues { it.value.randomValue() }
}