package tech.sabai.contracteer.core.swagger.converter

import io.swagger.v3.oas.models.media.IntegerSchema
import tech.sabai.contracteer.core.datatype.IntegerDataType
import tech.sabai.contracteer.core.normalize
import tech.sabai.contracteer.core.swagger.safeEnum
import tech.sabai.contracteer.core.swagger.safeExclusiveMaximum
import tech.sabai.contracteer.core.swagger.safeExclusiveMinimum
import tech.sabai.contracteer.core.swagger.safeNullable
import java.math.BigDecimal

object IntegerSchemaConverter {
  fun convert(schema: IntegerSchema) =
    IntegerDataType.create(
      name = schema.name,
      isNullable = schema.safeNullable(),
      minimum = schema.minimum,
      maximum = schema.maximum,
      exclusiveMinimum = schema.safeExclusiveMinimum(),
      exclusiveMaximum = schema.safeExclusiveMaximum(),
      enum = schema.safeEnum().map { it.normalize() as BigDecimal? })
}