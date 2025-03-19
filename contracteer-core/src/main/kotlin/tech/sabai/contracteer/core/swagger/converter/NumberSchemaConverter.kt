package tech.sabai.contracteer.core.swagger.converter

import io.swagger.v3.oas.models.media.NumberSchema
import tech.sabai.contracteer.core.datatype.NumberDataType
import tech.sabai.contracteer.core.swagger.safeEnum
import tech.sabai.contracteer.core.swagger.safeExclusiveMaximum
import tech.sabai.contracteer.core.swagger.safeExclusiveMinimum
import tech.sabai.contracteer.core.swagger.safeNullable

internal object NumberSchemaConverter {
  fun convert(schema: NumberSchema) =
    NumberDataType.create(
      name = schema.name,
      isNullable = schema.safeNullable(),
      minimum = schema.minimum,
      maximum = schema.maximum,
      exclusiveMinimum = schema.safeExclusiveMinimum(),
      exclusiveMaximum = schema.safeExclusiveMaximum(),
      enum = schema.safeEnum())
}