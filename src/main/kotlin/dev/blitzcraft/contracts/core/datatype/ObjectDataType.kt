package dev.blitzcraft.contracts.core.datatype

import dev.blitzcraft.contracts.core.Property
import io.swagger.v3.oas.models.media.ObjectSchema

data class ObjectDataType(val properties: Map<String, Property>): DataType<Map<String, Any>> {

  constructor(schema: ObjectSchema): this(schema.properties.mapValues { Property(DataType.from(it.value)) })

  override fun nextValue(): Map<String, Any> {
    return properties.mapValues { it.value.dataType.nextValue() }
  }

  override fun regexPattern(): String {
    throw RuntimeException("String Pattern for Object is not defined")
  }
}