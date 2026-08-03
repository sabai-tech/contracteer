package dev.contracteer.core.swagger.datatype

import io.swagger.v3.oas.models.media.Schema
import dev.contracteer.core.Result
import dev.contracteer.core.Result.Companion.failure
import dev.contracteer.core.combineResults
import dev.contracteer.core.datatype.AnyDataType
import dev.contracteer.core.datatype.DataType
import dev.contracteer.core.datatype.Discriminator
import dev.contracteer.core.datatype.OneOfDataType
import dev.contracteer.core.result
import dev.contracteer.core.swagger.effectiveEnum
import dev.contracteer.core.swagger.isNullable

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
      schema.rejectNullBranchAgainstOuterType(subTypes, "oneOf").bind()
      val enum = schema.effectiveEnum().bind()
      OneOfDataType.create(
        name = schema.name,
        subTypes = subTypes,
        discriminator = discriminator(schema),
        outerIsNullable = schema.isNullable(),
        enum = enum
      ).bind()
    }
}
