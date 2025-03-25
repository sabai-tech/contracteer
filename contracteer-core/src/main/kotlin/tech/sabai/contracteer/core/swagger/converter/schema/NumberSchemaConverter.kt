package tech.sabai.contracteer.core.swagger.converter.schema

import io.swagger.v3.oas.models.media.Schema
import tech.sabai.contracteer.core.datatype.NumberDataType
import tech.sabai.contracteer.core.swagger.safeEnum
import tech.sabai.contracteer.core.swagger.safeExclusiveMaximum
import tech.sabai.contracteer.core.swagger.safeExclusiveMinimum
import tech.sabai.contracteer.core.swagger.safeNullable
import java.math.BigDecimal

internal object NumberSchemaConverter {
  fun convert(schema: Schema<BigDecimal>) =
    NumberDataType.create(
      name = schema.name,
      isNullable = schema.safeNullable(),
      minimum = schema.minimum,
      maximum = schema.maximum,
      exclusiveMinimum = schema.safeExclusiveMinimum(),
      exclusiveMaximum = schema.safeExclusiveMaximum(),
      enum = schema.safeEnum())
}