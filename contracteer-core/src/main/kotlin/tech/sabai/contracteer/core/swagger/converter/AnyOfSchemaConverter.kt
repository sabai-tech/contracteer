package tech.sabai.contracteer.core.swagger.converter

import io.swagger.v3.oas.models.media.ComposedSchema
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.combineResults
import tech.sabai.contracteer.core.datatype.AnyOfDataType
import tech.sabai.contracteer.core.swagger.safeEnum
import tech.sabai.contracteer.core.swagger.safeNullable

internal object AnyOfSchemaConverter {

  fun convert(schema: ComposedSchema, recursiveDepth: Int) =
    if (schema.anyOf == null) failure("'anyOf' must be not null")
    else schema.anyOf
      .mapIndexed { index, sub ->
        SchemaConverter.convertToDataType(sub, "${schema.name} - anyOf #$index", recursiveDepth - 1)
      }
      .combineResults()
      .flatMap { subTypes ->
        AnyOfDataType.create(
          name = schema.name,
          subTypes = subTypes!!,
          discriminator = SchemaConverter.convertToDiscriminator(schema),
          isNullable = schema.safeNullable(),
          enum = schema.safeEnum())
      }
}
