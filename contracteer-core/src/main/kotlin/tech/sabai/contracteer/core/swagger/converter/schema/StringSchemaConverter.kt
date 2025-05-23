package tech.sabai.contracteer.core.swagger.converter.schema

import io.swagger.v3.oas.models.media.Schema
import tech.sabai.contracteer.core.datatype.StringDataType
import tech.sabai.contracteer.core.swagger.safeEnum
import tech.sabai.contracteer.core.swagger.safeNullable

internal object StringSchemaConverter {
  fun convert(schema: Schema<String>, openApiType: String) =
    StringDataType.create(
      name = schema.name,
      openApiType,
      isNullable = schema.safeNullable(),
      minLength = schema.minLength,
      maxLength = schema.maxLength,
      enum = schema.safeEnum())
}