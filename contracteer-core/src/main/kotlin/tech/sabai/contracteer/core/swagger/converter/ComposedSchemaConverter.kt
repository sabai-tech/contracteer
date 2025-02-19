package tech.sabai.contracteer.core.swagger.converter

import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.Schema
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.combineResults
import tech.sabai.contracteer.core.datatype.*
import tech.sabai.contracteer.core.swagger.SharedComponents
import tech.sabai.contracteer.core.swagger.safeEnum
import tech.sabai.contracteer.core.swagger.safeNullable

object ComposedSchemaConverter {

  fun convert(schema: ComposedSchema) =
    when {
      schema.oneOf?.isNotEmpty() == true -> convertToStructuredObjectDataType(
        schema,
        subSchemas = schema.oneOf!!,
        typeName = "oneOf",
        buildDataType = { subTypes, discriminator ->
          OneOfDataType.create(schema.name, subTypes, discriminator, schema.safeNullable(), schema.safeEnum())
        }
      )

      schema.anyOf?.isNotEmpty() == true -> convertToStructuredObjectDataType(
        schema,
        subSchemas = schema.anyOf!!,
        typeName = "anyOf",
        buildDataType = { subTypes, discriminator ->
          AnyOfDataType.create(schema.name, subTypes, discriminator, schema.safeNullable(), schema.safeEnum())
        }
      )

      schema.allOf?.isNotEmpty() == true -> convertToStructuredObjectDataType(
        schema,
        subSchemas = schema.allOf!!,
        typeName = "allOf",
        buildDataType = { subTypes, _ ->
          AllOfDataType.create(schema.name, subTypes, schema.safeNullable(), schema.safeEnum())
        }
      )

      else                               -> failure("Schema ${schema::class.java} is not yet supported")
    }

  @Suppress("UNCHECKED_CAST")
  private inline fun <reified T: StructuredObjectDataType> convertToStructuredObjectDataType(
    schema: ComposedSchema,
    subSchemas: List<Schema<Any>>,
    typeName: String,
    buildDataType: (subTypes: List<StructuredObjectDataType>, discriminator: Discriminator?) -> Result<T>
  ): Result<T> {
    val subTypeResult = subSchemas.map { SchemaConverter.convert(it) }.combineResults()
    if (subTypeResult.isFailure()) return subTypeResult.retypeError()

    val convertedSubDataTypes = subTypeResult.value!!
    val nonObjectDataTypes = convertedSubDataTypes.filterNot { it is StructuredObjectDataType }
    if (nonObjectDataTypes.isNotEmpty()) return failure("Only schema of type 'object' are supported for '$typeName'")

    val discriminatorResult = convertDiscriminator(schema)
    if (discriminatorResult?.isFailure() == true) return discriminatorResult.retypeError()

    return buildDataType(convertedSubDataTypes as List<StructuredObjectDataType>,
                         discriminatorResult?.value)

  }

  private fun convertDiscriminator(schema: ComposedSchema): Result<Discriminator>? {
    val swaggerDiscriminator = schema.discriminator ?: return null

    val mapped = swaggerDiscriminator.mapping.mapValues {
      SchemaConverter.convert(SharedComponents.findSchema(it.value))
    }

    val combined = mapped.values.combineResults()
    if (combined.isFailure()) return combined.retypeError()

    return success(Discriminator(swaggerDiscriminator.propertyName,
                                 mapped.mapValues { (_, result) -> result.value!! as ObjectDataType }))
  }
}
