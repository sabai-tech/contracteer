package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.accumulate

class ObjectDataType(name: String = "Inline 'object' Schema",
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
    }.map { value }

  private fun isRequired(key: String) =
    requiredProperties.contains(key)

  override fun randomValue() =
    properties.mapValues { it.value.randomValue() }
}