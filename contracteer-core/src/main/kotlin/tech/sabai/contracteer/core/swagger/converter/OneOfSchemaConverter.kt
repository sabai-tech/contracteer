package tech.sabai.contracteer.core.swagger.converter

import io.swagger.v3.oas.models.media.ComposedSchema
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.combineResults
import tech.sabai.contracteer.core.datatype.OneOfDataType
import tech.sabai.contracteer.core.swagger.safeEnum
import tech.sabai.contracteer.core.swagger.safeNullable

object OneOfSchemaConverter {
  fun convert(schema: ComposedSchema, recursiveDepth: Int) =
    if (schema.oneOf == null) failure("'anyOf' must be not null")
    else schema.oneOf
      .mapIndexed { index, sub ->
        SchemaConverter.convertToDataType(sub, "${schema.name} - allOf #$index", recursiveDepth - 1)
      }
      .combineResults()
      .flatMap { subTypes ->
        OneOfDataType.create(
          name = schema.name,
          subTypes = subTypes!!,
          discriminator = SchemaConverter.convertToDiscriminator(schema),
          isNullable = schema.safeNullable(),
          enum = schema.safeEnum())
      }
}
