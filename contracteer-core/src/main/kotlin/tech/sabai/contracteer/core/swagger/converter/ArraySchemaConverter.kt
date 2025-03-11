package tech.sabai.contracteer.core.swagger.converter

import io.swagger.v3.oas.models.media.ArraySchema
import tech.sabai.contracteer.core.datatype.ArrayDataType
import tech.sabai.contracteer.core.normalize
import tech.sabai.contracteer.core.swagger.convertToDataType
import tech.sabai.contracteer.core.swagger.safeEnum
import tech.sabai.contracteer.core.swagger.safeNullable

object ArraySchemaConverter {
  fun convert(schema: ArraySchema) =
    schema.items.convertToDataType().flatMap { itemDataType ->
      ArrayDataType.create(
        name = schema.name,
        itemDataType = itemDataType!!,
        isNullable = schema.safeNullable(),
        enum = schema.safeEnum().map { it.normalize() }
      )
    }
}