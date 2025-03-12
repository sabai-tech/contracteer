package tech.sabai.contracteer.core.swagger.converter

import io.swagger.v3.oas.models.media.ComposedSchema
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.combineResults
import tech.sabai.contracteer.core.datatype.AllOfDataType
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.swagger.*

@Suppress("UNCHECKED_CAST")
object AllOfSchemaConverter {
  fun convert(composedSchema: ComposedSchema): Result<AllOfDataType> {
    if (composedSchema.allOf == null) return failure("'allOf' must be defined.")

    return composedSchema.allOf
      .map { it.fullyResolve() }
      .combineResults()
      .flatMap { schemas ->
        val subTypesResult = schemas!!.map { it.convertToDataType() }.combineResults()
        val discriminators = schemas
          .filter { it.name != null && it.name != "Inline Schema" }
          .mapNotNull { it.toContracteerDiscriminator() }

        when {
          subTypesResult.isFailure()                             -> subTypesResult.retypeError()
          subTypesResult.value!!.any { !it.isFullyStructured() } -> failure("Only 'object', 'allOf', 'anyOf' and 'oneOf' schemas are supported for 'allOf'")
          discriminators.size > 1                                -> failure("Only 1 discriminator is allowed")
          else                                                   -> {
            AllOfDataType.create(
              name = composedSchema.name,
              subTypes = subTypesResult.value.map { it as DataType<Map<String, Any?>> },
              isNullable = composedSchema.safeNullable(),
              discriminator = discriminators.firstOrNull(),
              enum = composedSchema.safeEnum())
          }
        }
      }
  }
}
