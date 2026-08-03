package dev.contracteer.core.swagger.datatype

import io.swagger.v3.oas.models.media.Schema
import dev.contracteer.core.Result
import dev.contracteer.core.Result.Companion.failure
import dev.contracteer.core.combineResults
import dev.contracteer.core.datatype.AllOfDataType
import dev.contracteer.core.datatype.AnyDataType
import dev.contracteer.core.datatype.DataType
import dev.contracteer.core.datatype.Discriminator
import dev.contracteer.core.result
import dev.contracteer.core.swagger.effectiveEnum
import dev.contracteer.core.swagger.isNullable

internal object AllOfDataTypeConverter {

  fun convert(schema: Schema<*>,
              convert: (Schema<*>, String) -> Result<DataType<out Any>>,
              discriminator: (Schema<*>) -> Discriminator?): Result<AllOfDataType> {
    if (schema.allOf == null) return failure("'allOf' must be defined.")

    val subTypeResults = schema.allOf
      .mapIndexed { index, subSchema -> convert(subSchema, "allOf #$index") }

    val siblingResult = ObjectDataTypeConverter.convertSiblingObject(schema, convert)

    return result {
      val subDataTypes = (subTypeResults + listOfNotNull(siblingResult))
        .combineResults()
        .bind()
        .filter { it !is AnyDataType }
      schema.rejectAllOfNullBranch(subDataTypes).bind()
      val enum = schema.effectiveEnum().bind()
      val discriminators = schema.allOf.mapNotNull { discriminator(it) }
      when {
        discriminators.size > 1 -> failure<AllOfDataType>("Only 1 discriminator is allowed in 'allOf'.").bind()
        else                    ->
          AllOfDataType.create(
            name = schema.name,
            subTypes = subDataTypes,
            outerIsNullable = schema.isNullable(),
            discriminator = discriminators.firstOrNull(),
            enum = enum
          ).bind()
      }
    }
  }
}
