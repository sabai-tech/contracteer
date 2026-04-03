package tech.sabai.contracteer.core.swagger.datatype

import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.Schema
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.combineResults
import tech.sabai.contracteer.core.datatype.AllOfDataType
import tech.sabai.contracteer.core.datatype.AnyDataType
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.datatype.Discriminator
import tech.sabai.contracteer.core.swagger.safeEnum
import tech.sabai.contracteer.core.swagger.safeNullable

internal object AllOfDataTypeConverter {

  fun convert(schema: ComposedSchema,
              maxRecursiveDepth: Int,
              convert: (Schema<*>, String, Int) -> Result<DataType<out Any>>,
              discriminator: (Schema<*>) -> Discriminator?): Result<AllOfDataType> {
    if (schema.allOf == null) return failure("'allOf' must be defined.")

    val subTypeResults = schema.allOf
      .mapIndexed { index, subSchema -> convert(subSchema, "allOf #$index", maxRecursiveDepth - 1) }

    val siblingResult = ObjectDataTypeConverter.convertSiblingObject(schema, maxRecursiveDepth, convert)

    return (subTypeResults + listOfNotNull(siblingResult))
      .combineResults()
      .map { subDataTypes -> subDataTypes!!.filter { it !is AnyDataType } }
      .flatMap { subDataTypes ->
        val discriminators = schema.allOf.mapNotNull { discriminator(it) }
        when {
          discriminators.size > 1 -> failure("Only 1 discriminator is allowed in 'allOf'.")
          else                    ->
            AllOfDataType.create(
              name = schema.name,
              subTypes = subDataTypes!!,
              isNullable = schema.safeNullable(),
              discriminator = discriminators.firstOrNull(),
              enum = schema.safeEnum())
        }
      }.forProperty(schema.name)
  }
}
