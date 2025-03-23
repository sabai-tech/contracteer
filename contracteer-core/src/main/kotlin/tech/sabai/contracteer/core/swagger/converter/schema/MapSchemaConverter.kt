package tech.sabai.contracteer.core.swagger.converter.schema

import io.swagger.v3.oas.models.media.MapSchema
import io.swagger.v3.oas.models.media.Schema
import tech.sabai.contracteer.core.datatype.MapDataType
import tech.sabai.contracteer.core.normalize
import tech.sabai.contracteer.core.swagger.safeEnum
import tech.sabai.contracteer.core.swagger.safeNullable
import tech.sabai.contracteer.core.swagger.safeProperties

object MapSchemaConverter {
  fun convert(schema: MapSchema, maxRecursiveDepth: Int) =
    SchemaConverter
      .convertToDataType(schema.additionalProperties as Schema<*>, schema.name, maxRecursiveDepth - 1)
      .flatMap {
        MapDataType.create(
          name = schema.name,
          valueDataType = it!!,
          properties = schema.safeProperties().keys,
          requiredProperties = schema.required?.toSet() ?: emptySet(),
          isNullable = schema.safeNullable(),
          enum = schema.safeEnum().map { it.normalize() }
        )
      }.forProperty(schema.name)
}