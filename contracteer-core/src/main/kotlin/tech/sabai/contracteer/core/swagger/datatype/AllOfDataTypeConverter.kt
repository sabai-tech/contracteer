package tech.sabai.contracteer.core.swagger.datatype

import io.swagger.v3.oas.models.media.Schema
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.combineResults
import tech.sabai.contracteer.core.datatype.AllOfDataType
import tech.sabai.contracteer.core.datatype.AnyDataType
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.datatype.Discriminator
import tech.sabai.contracteer.core.result
import tech.sabai.contracteer.core.swagger.effectiveEnum
import tech.sabai.contracteer.core.swagger.isNullable

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
