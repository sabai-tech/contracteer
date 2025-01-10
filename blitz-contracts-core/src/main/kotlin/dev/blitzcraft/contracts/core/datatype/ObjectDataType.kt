package dev.blitzcraft.contracts.core.datatype

import dev.blitzcraft.contracts.core.Result
import dev.blitzcraft.contracts.core.Result.Companion.failure
import dev.blitzcraft.contracts.core.Result.Companion.success
import dev.blitzcraft.contracts.core.accumulate

class ObjectDataType(name: String = "Inline Schema",
                     val properties: Map<String, DataType<*>>,
                     val requiredProperties: Set<String> = emptySet(),
                     isNullable: Boolean = false): StructuredObjectDataType(name, "object", isNullable) {

  override fun doValidate(value: Map<String, Any?>): Result<Map<String, Any?>> =
    properties.accumulate {
      when {
        !value.containsKey(it.key) && !isRequired(it.key) -> success(value)
        !value.containsKey(it.key)                        -> failure(it.key, "is required")
        else                                              -> it.value.validate(value[it.key]).forProperty(it.key)
      }
    }.mapSuccess { success(value) }

  private fun isRequired(key: String) =
    requiredProperties.contains(key)

  override fun randomValue() =
    properties.mapValues { it.value.randomValue() }
}