package tech.sabai.contracteer.core.swagger.converter

import io.swagger.v3.oas.models.media.ObjectSchema
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.combineResults
import tech.sabai.contracteer.core.datatype.ObjectDataType
import tech.sabai.contracteer.core.swagger.safeNullable

internal object ObjectSchemaConverter {

  fun convert(schema: ObjectSchema): Result<ObjectDataType> {
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