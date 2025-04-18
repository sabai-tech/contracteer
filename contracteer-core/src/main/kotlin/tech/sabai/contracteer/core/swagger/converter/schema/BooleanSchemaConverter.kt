package tech.sabai.contracteer.core.swagger.converter.schema

import io.swagger.v3.oas.models.media.Schema
import tech.sabai.contracteer.core.datatype.BooleanDataType
import tech.sabai.contracteer.core.swagger.safeEnum
import tech.sabai.contracteer.core.swagger.safeNullable

internal object BooleanSchemaConverter {
  fun convert(schema: Schema<Boolean>) =
    BooleanDataType.create(
      name = schema.name,
      isNullable = schema.safeNullable(),
      enum = schema.safeEnum()
    )
}