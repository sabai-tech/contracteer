package tech.sabai.contracteer.core.swagger.datatype

import io.swagger.v3.oas.models.media.Schema
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.datatype.ArrayDataType
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.result
import tech.sabai.contracteer.core.swagger.effectiveEnum
import tech.sabai.contracteer.core.swagger.isNullable

internal object ArrayDataTypeConverter {
  fun convert(
    schema: Schema<*>,
    convert: (Schema<*>, String) -> Result<DataType<out Any>>
  ) =
    result {
      val itemDataType = convert(schema.items, schema.name).bind()
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
