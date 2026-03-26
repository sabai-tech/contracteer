package tech.sabai.contracteer.core.swagger.datatype

import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.Schema
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.datatype.ArrayDataType
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.normalize
import tech.sabai.contracteer.core.swagger.safeEnum
import tech.sabai.contracteer.core.swagger.safeNullable

internal object ArrayDataTypeConverter {
  fun convert(
    schema: ArraySchema,
    maxRecursiveDepth: Int,
    convert: (Schema<*>, String, Int) -> Result<DataType<out Any>>
  ) =
    convert(schema.items, schema.name, maxRecursiveDepth - 1)
      .flatMap { itemDataType ->
        ArrayDataType.create(
          name = schema.name,
          itemDataType = itemDataType!!,
          isNullable = schema.safeNullable(),
          enum = schema.safeEnum().map { it.normalize() },
          minItems = schema.minItems,
          maxItems = schema.maxItems,
          uniqueItems = schema.uniqueItems ?: false
        )
      }.forProperty(schema.name)
}