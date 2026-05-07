package tech.sabai.contracteer.core.swagger.datatype

import io.swagger.v3.oas.models.media.Schema
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.combineResults
import tech.sabai.contracteer.core.datatype.AnyDataType
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.datatype.Discriminator
import tech.sabai.contracteer.core.datatype.OneOfDataType
import tech.sabai.contracteer.core.result
import tech.sabai.contracteer.core.swagger.effectiveEnum
import tech.sabai.contracteer.core.swagger.isNullable

internal object OneOfDataTypeConverter {
  fun convert(
    schema: Schema<*>,
    convert: (Schema<*>, String) -> Result<DataType<out Any>>,
    discriminator: (Schema<*>) -> Discriminator?
  ) =
    if (schema.oneOf == null) failure("'oneOf' must be defined")
    else result {
      val subTypes = schema.oneOf
        .mapIndexed { index, sub -> convert(sub, "oneOf #$index") }
        .combineResults()
        .bind()
        .filter { it !is AnyDataType }
      val enum = schema.effectiveEnum().bind()
      OneOfDataType.create(
        name = schema.name,
        subTypes = subTypes,
        discriminator = discriminator(schema),
        isNullable = schema.isNullable(),
        enum = enum
      ).bind()
    }
}
