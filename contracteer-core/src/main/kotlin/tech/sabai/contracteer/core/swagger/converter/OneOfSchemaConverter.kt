package tech.sabai.contracteer.core.swagger.converter

import io.swagger.v3.oas.models.media.ComposedSchema
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.combineResults
import tech.sabai.contracteer.core.datatype.OneOfDataType
import tech.sabai.contracteer.core.swagger.safeEnum
import tech.sabai.contracteer.core.swagger.safeNullable

object OneOfSchemaConverter {
  fun convert(schema: ComposedSchema, maxRecursiveDepth: Int) =
    if (schema.oneOf == null) failure("'oneOf' must be not null")
    else schema.oneOf
      .mapIndexed { index, sub ->
        val convertToDataType = SchemaConverter.convertToDataType(sub, "oneOf #$index", maxRecursiveDepth - 1)
        convertToDataType
      }
      .combineResults()
      .flatMap { subTypes ->
        OneOfDataType.create(
          name = schema.name,
          subTypes = subTypes!!,
          discriminator = SchemaConverter.convertToDiscriminator(schema),
          isNullable = schema.safeNullable(),
          enum = schema.safeEnum()
        )
      }.forProperty("${schema.name}")
}
