package tech.sabai.contracteer.core.swagger.converter

import io.swagger.v3.oas.models.media.ComposedSchema
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.combineResults
import tech.sabai.contracteer.core.datatype.AnyOfDataType
import tech.sabai.contracteer.core.swagger.toContracteerDiscriminator
import tech.sabai.contracteer.core.swagger.convertToDataType
import tech.sabai.contracteer.core.swagger.safeEnum
import tech.sabai.contracteer.core.swagger.safeNullable

object AnyOfSchemaConverter {

  fun convert(schema: ComposedSchema) =
    if (schema.anyOf == null) failure("'anyOf' must be not null")
    else schema.anyOf
      .map { it.convertToDataType() }
      .combineResults()
      .flatMap { subTypes ->
        AnyOfDataType.create(
          name = schema.name,
          subTypes = subTypes!!,
          discriminator = schema.toContracteerDiscriminator(),
          isNullable = schema.safeNullable(),
          enum = schema.safeEnum())
      }
}
