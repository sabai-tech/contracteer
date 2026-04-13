package tech.sabai.contracteer.core.swagger.datatype

import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.Schema
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.combineResults
import tech.sabai.contracteer.core.datatype.AnyDataType
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.datatype.Discriminator
import tech.sabai.contracteer.core.datatype.OneOfDataType
import tech.sabai.contracteer.core.swagger.safeEnum
import tech.sabai.contracteer.core.swagger.safeNullable

internal object OneOfDataTypeConverter {
  fun convert(
    schema: ComposedSchema,
    convert: (Schema<*>, String) -> Result<DataType<out Any>>,
    discriminator: (Schema<*>) -> Discriminator?
  ) =
    if (schema.oneOf == null) failure("'oneOf' must be defined")
    else schema.oneOf
      .mapIndexed { index, sub ->
        convert(sub, "oneOf #$index")
      }
      .combineResults()
      .map { subTypes -> subTypes.filter { it !is AnyDataType } }
      .flatMap { subTypes ->
        OneOfDataType.create(
          name = schema.name,
          subTypes = subTypes,
          discriminator = discriminator(schema),
          isNullable = schema.safeNullable(),
          enum = schema.safeEnum()
        )
      }
}
