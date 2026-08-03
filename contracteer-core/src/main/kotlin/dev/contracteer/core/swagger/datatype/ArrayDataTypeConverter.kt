package dev.contracteer.core.swagger.datatype

import io.swagger.v3.oas.models.media.Schema
import dev.contracteer.core.Result
import dev.contracteer.core.datatype.AnyDataType
import dev.contracteer.core.datatype.ArrayDataType
import dev.contracteer.core.datatype.DataType
import dev.contracteer.core.result
import dev.contracteer.core.swagger.effectiveEnum
import dev.contracteer.core.swagger.isNullable

internal object ArrayDataTypeConverter {
  fun convert(
    schema: Schema<*>,
    convert: (Schema<*>, String) -> Result<DataType<out Any>>
  ) =
    result {
      val itemDataType =
        if (schema.items != null) convert(schema.items, schema.name).bind()
        else AnyDataType
      val enum = schema.effectiveEnum().bind()
      ArrayDataType.create(
        name = schema.name,
        itemDataType = itemDataType,
        isNullable = schema.isNullable(),
        enum = enum,
        minItems = schema.minItems,
        maxItems = schema.maxItems,
        uniqueItems = schema.uniqueItems ?: false
      ).bind()
    }
}
