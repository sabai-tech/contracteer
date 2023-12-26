package dev.blitzcraft.contracts.core.datatype

import com.fasterxml.jackson.databind.node.ObjectNode
import dev.blitzcraft.contracts.core.CompositeValidationResult
import dev.blitzcraft.contracts.core.Property
import dev.blitzcraft.contracts.core.SimpleValidationResult
import io.swagger.v3.oas.models.media.ObjectSchema

data class ObjectDataType(val properties: List<Property>): DataType<Map<String, Any>> {

  constructor(schema: ObjectSchema): this(schema.properties.map { Property(it.key, DataType.from(it.value)) })

  override fun nextValue(): Map<String, Any> = properties.associate { it.name to it.dataType.nextValue() }

  override fun regexPattern() = throw UnsupportedOperationException("String Pattern for Object is not defined")

  override fun validateValue(value: Any) = when (value) {
    is ObjectNode -> CompositeValidationResult(properties.validatePropertyOf(value))
    is Map<*, *>  -> CompositeValidationResult(properties.validatePropertyOf(value))
    else          -> SimpleValidationResult("Wrong type. Expected type: Object")
  }

  override fun parseAndValidate(stringValue: String) = SimpleValidationResult("Parsing an Object is not yet supported")

  private fun List<Property>.validatePropertyOf(value: ObjectNode) =
    map {
      if (value.has(it.name).not() && it.required) SimpleValidationResult(it.name, "is required")
      else it.validateValue(value[it.name])
    }

  private fun List<Property>.validatePropertyOf(value: Map<*, *>) =
    map {
      if (value.containsValue(it.name).not() && it.required) SimpleValidationResult(it.name, "is required")
      else it.validateValue(value[it.name])
    }
}