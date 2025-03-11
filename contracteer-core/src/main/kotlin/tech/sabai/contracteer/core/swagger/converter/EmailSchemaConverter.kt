package tech.sabai.contracteer.core.swagger.converter

import io.swagger.v3.oas.models.media.EmailSchema
import tech.sabai.contracteer.core.datatype.EmailDataType
import tech.sabai.contracteer.core.swagger.safeEnum
import tech.sabai.contracteer.core.swagger.safeNullable

object EmailSchemaConverter {
  fun convert(schema: EmailSchema) =
    EmailDataType.create(
      name = schema.name,
      isNullable = schema.safeNullable(),
      minLength = schema.minLength,
      maxLength = schema.maxLength,
      enum = schema.safeEnum(),
    )
}