package tech.sabai.contracteer.core.swagger.converter

import io.swagger.v3.oas.models.media.BinarySchema
import tech.sabai.contracteer.core.datatype.BinaryDataType
import tech.sabai.contracteer.core.swagger.safeEnum
import tech.sabai.contracteer.core.swagger.safeNullable

internal object BinarySchemaConverter {
  fun convert(schema: BinarySchema) =
    BinaryDataType.create(
      name = schema.name,
      isNullable = schema.safeNullable(),
      minLength = schema.minLength,
      maxLength = schema.maxLength,
      enum = schema.safeEnum().map { String(it) }
    )
}