package tech.sabai.contracteer.core.swagger.datatype

import io.swagger.v3.oas.models.media.Schema
import tech.sabai.contracteer.core.datatype.StringDataType
import tech.sabai.contracteer.core.swagger.safeEnum
import tech.sabai.contracteer.core.swagger.isNullable

internal object StringDataTypeConverter {
  fun convert(schema: Schema<String>, openApiType: String) =
    StringDataType.create(
      name = schema.name,
      openApiType,
      isNullable = schema.isNullable(),
      minLength = schema.minLength ?: 0,
      maxLength = schema.maxLength,
      pattern = schema.pattern,
      enum = schema.safeEnum())
}