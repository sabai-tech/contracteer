package tech.sabai.contracteer.core.swagger.converter

import io.swagger.v3.oas.models.media.ObjectSchema
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.combineResults
import tech.sabai.contracteer.core.datatype.ObjectDataType
import tech.sabai.contracteer.core.normalize
import tech.sabai.contracteer.core.swagger.safeEnum
import tech.sabai.contracteer.core.swagger.safeNullable
import tech.sabai.contracteer.core.swagger.safeProperties

object ObjectSchemaConverter {
  fun convert(schema: ObjectSchema, recursiveDepth: Int): Result<ObjectDataType> {
    val propertyDataTypeResults =
      schema.safeProperties().mapValues { SchemaConverter.convertToDataType(it.value, recursiveDepth = recursiveDepth - 1) }
    return propertyDataTypeResults.values
      .combineResults()
      .flatMap {
        ObjectDataType.create(
          name = schema.name,
          properties = propertyDataTypeResults.mapValues { it.value.value!! },
          requiredProperties = schema.required?.toSet() ?: emptySet(),
          isNullable = schema.safeNullable(),
          enum = schema.safeEnum().map { it.normalize() }
        )
      }
  }
}