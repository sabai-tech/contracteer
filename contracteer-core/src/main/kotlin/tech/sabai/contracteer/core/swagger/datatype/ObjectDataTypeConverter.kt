package tech.sabai.contracteer.core.swagger.datatype

import io.swagger.v3.oas.models.media.Schema
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.combineResults
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.datatype.ObjectDataType
import tech.sabai.contracteer.core.normalize
import tech.sabai.contracteer.core.swagger.safeEnum
import tech.sabai.contracteer.core.swagger.safeNullable
import tech.sabai.contracteer.core.swagger.safeProperties

internal object ObjectDataTypeConverter {
  fun convert(
    schema: Schema<*>,
    maxRecursiveDepth: Int,
    convert: (Schema<*>, String, Int) -> Result<DataType<out Any>>
  ): Result<ObjectDataType> {
    val propertyDataTypeResults = schema
      .safeProperties()
      .mapValues { (name, subSchema) -> convert(subSchema, name, maxRecursiveDepth - 1).forProperty(name) }

    val allowAdditionalProperties = (schema.additionalProperties as? Boolean?) != false
    val additionalPropertiesSchema = schema.additionalProperties.takeIf { it !is Boolean } as Schema<*>?
    val additionalPropertiesDataTypeResult =
      if (additionalPropertiesSchema == null) success()
      else convert(additionalPropertiesSchema, "additionalProperties", maxRecursiveDepth - 1)

    return propertyDataTypeResults.values
      .combineResults()
      .flatMap {
        additionalPropertiesDataTypeResult.flatMap { additionalPropertiesDataType ->
          ObjectDataType.create(
            name = schema.name,
            properties = propertyDataTypeResults.mapValues { it.value.value!! },
            requiredProperties = schema.required?.toSet() ?: emptySet(),
            allowAdditionalProperties = allowAdditionalProperties,
            additionalPropertiesDataType = additionalPropertiesDataType,
            isNullable = schema.safeNullable(),
            enum = schema.safeEnum().map { it.normalize() }
          )
        }
      }.forProperty(schema.name)
  }
}
