package tech.sabai.contracteer.core.swagger.converter

import io.swagger.v3.oas.models.media.ComposedSchema
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.combineResults
import tech.sabai.contracteer.core.datatype.OneOfDataType
import tech.sabai.contracteer.core.swagger.toContracteerDiscriminator
import tech.sabai.contracteer.core.swagger.convertToDataType
import tech.sabai.contracteer.core.swagger.safeEnum
import tech.sabai.contracteer.core.swagger.safeNullable

object OneOfSchemaConverter {
  fun convert(schema: ComposedSchema) =
    if (schema.oneOf == null) failure("'anyOf' must be not null")
    else schema.oneOf
      .map { it.convertToDataType() }
      .combineResults()
      .flatMap { subTypes ->
        OneOfDataType.create(
          name = schema.name,
          subTypes = subTypes!!,
          discriminator = schema.toContracteerDiscriminator(),
          isNullable = schema.safeNullable(),
          enum = schema.safeEnum())
      }
}
