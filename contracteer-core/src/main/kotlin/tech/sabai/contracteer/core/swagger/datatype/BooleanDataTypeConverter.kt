package tech.sabai.contracteer.core.swagger.datatype

import io.swagger.v3.oas.models.media.Schema
import tech.sabai.contracteer.core.datatype.BooleanDataType
import tech.sabai.contracteer.core.swagger.safeEnum
import tech.sabai.contracteer.core.swagger.safeNullable

internal object BooleanDataTypeConverter {
  fun convert(schema: Schema<Boolean>) =
    BooleanDataType.create(
      name = schema.name,
      isNullable = schema.safeNullable(),
      enum = schema.safeEnum()
    )
}