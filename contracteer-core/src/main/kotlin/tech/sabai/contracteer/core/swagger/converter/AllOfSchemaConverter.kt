package tech.sabai.contracteer.core.swagger.converter

import io.swagger.v3.oas.models.media.ComposedSchema
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.combineResults
import tech.sabai.contracteer.core.datatype.AllOfDataType
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.swagger.*

@Suppress("UNCHECKED_CAST")
internal object AllOfSchemaConverter {
  fun convert(schema: ComposedSchema, maxRecursiveDepth: Int): Result<AllOfDataType> {
    if (schema.allOf == null) return failure("'allOf' must be defined.")

    return schema.allOf
      .mapIndexed { index, subSchema ->
        SchemaConverter.convertToDataType(subSchema, "allOf #$index", maxRecursiveDepth - 1)
      }.combineResults()
      .flatMap { subDataTypes ->
        val discriminators = schema.allOf.mapNotNull { SchemaConverter.convertToDiscriminator(it) }
        when {
          subDataTypes!!.any { !it.isFullyStructured() } -> failure("Only 'object', 'allOf', 'anyOf' and 'oneOf' schemas are supported for 'allOf'")
          discriminators.size > 1                        -> failure("Only 1 discriminator is allowed")
          else                                           -> {
            AllOfDataType.create(
              name = schema.name,
              subTypes = subDataTypes.map { it as DataType<Map<String, Any?>> },
              isNullable = schema.safeNullable(),
              discriminator = discriminators.firstOrNull(),
              enum = schema.safeEnum())
          }
        }
      }.forProperty(schema.name)
  }
}
