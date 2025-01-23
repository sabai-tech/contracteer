package dev.blitzcraft.contracts.core.loader.swagger.converter

import dev.blitzcraft.contracts.core.Result.Companion.success
import dev.blitzcraft.contracts.core.combineResults
import dev.blitzcraft.contracts.core.datatype.ObjectDataType
import dev.blitzcraft.contracts.core.loader.swagger.safeNullable
import io.swagger.v3.oas.models.media.ObjectSchema

internal object ObjectSchemaConverter {

  fun convert(schema: ObjectSchema): dev.blitzcraft.contracts.core.Result<ObjectDataType> {
    val propertyDataTypeResults = schema.properties.mapValues { SchemaConverter.convert(it.value) }
    val combineResults = propertyDataTypeResults.values.combineResults()
    return when {
      combineResults.isSuccess() -> success(ObjectDataType(name = schema.name,
                                                           properties = propertyDataTypeResults.mapValues { it.value.value!! },
                                                           requiredProperties = schema.required?.toSet() ?: emptySet(),
                                                           isNullable = schema.safeNullable()))
      else                       -> combineResults.retypeError()
    }
  }
}