package tech.sabai.contracteer.core.swagger.converter.schema

import io.swagger.v3.oas.models.media.Schema
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.combineResults
import tech.sabai.contracteer.core.datatype.ObjectDataType
import tech.sabai.contracteer.core.normalize
import tech.sabai.contracteer.core.swagger.safeEnum
import tech.sabai.contracteer.core.swagger.safeNullable
import tech.sabai.contracteer.core.swagger.safeProperties

object ObjectSchemaConverter {
  fun convert(schema: Schema<*>, maxRecursiveDepth: Int): Result<ObjectDataType> {
    val propertyDataTypeResults = schema
      .safeProperties()
      .mapValues { (name, subSchema) -> SchemaConverter.convertToDataType(subSchema, name, maxRecursiveDepth - 1).forProperty(name) }

    val allowAdditionalProperties = (schema.additionalProperties as? Boolean?) != false
    val additionalPropertiesSchema = schema.additionalProperties.takeIf { it !is Boolean } as Schema<*>?
    val additionalPropertiesDataTypeResult =
      if (additionalPropertiesSchema == null) success()
      else SchemaConverter.convertToDataType(additionalPropertiesSchema, "additionalProperties", maxRecursiveDepth - 1)

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