package tech.sabai.contracteer.core.swagger.converter

import io.swagger.v3.oas.models.media.ComposedSchema
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.combineResults
import tech.sabai.contracteer.core.datatype.AllOfDataType
import tech.sabai.contracteer.core.datatype.CompositeDataType
import tech.sabai.contracteer.core.swagger.*

@Suppress("UNCHECKED_CAST")
object AllOfSchemaConverter {
  fun convert(composedSchema: ComposedSchema): Result<AllOfDataType> {
    val allOfSchemas = composedSchema.allOf ?: return failure("'allOf' must be defined.")

    val subTypesResult = allOfSchemas.map { it.convertToDataType() }.combineResults()
    if (subTypesResult.isFailure()) return subTypesResult.retypeError()
    if (subTypesResult.value!!.any { it !is CompositeDataType || !it.isStructured() }) return failure("Only 'object', 'allOf', 'anyOf' and 'oneOf' schemas are supported for 'allOf'")

    return allOfSchemas
      .map { it.fullyResolve() }
      .combineResults()
      .flatMap { schemas ->
        schemas!!
          .filter { it.name != null && it.name != "Inline Schema" }
          .mapNotNull { it.toContracteerDiscriminator() }
          .let { discriminators ->
            when {
              discriminators.size > 1 -> failure("Only 1 discriminator is allowed")
              else                    ->
                AllOfDataType.create(
                  name = composedSchema.name,
                  subTypes = subTypesResult.value.map { it as CompositeDataType<Map<String, Any?>> },
                  isNullable = composedSchema.safeNullable(),
                  discriminator = discriminators.firstOrNull(),
                  enum = composedSchema.safeEnum())
            }
          }
      }
  }
}
