package tech.sabai.contracteer.core.swagger.converter

import io.swagger.v3.oas.models.media.UUIDSchema
import tech.sabai.contracteer.core.datatype.UuidDataType
import tech.sabai.contracteer.core.swagger.safeEnum
import tech.sabai.contracteer.core.swagger.safeNullable

object UuidSchemaConverter {
  fun convert(schema: UUIDSchema) =
    UuidDataType.create(
      name = schema.name,
      isNullable = schema.safeNullable(),
      enum = schema.safeEnum().map { it.toString() }
    )
}