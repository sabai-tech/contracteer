package tech.sabai.contracteer.core.swagger.datatype

import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.Schema
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.accumulate
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.datatype.ObjectDataType
import tech.sabai.contracteer.core.normalize
import tech.sabai.contracteer.core.result
import tech.sabai.contracteer.core.swagger.safeEnum
import tech.sabai.contracteer.core.swagger.safeNullable
import tech.sabai.contracteer.core.swagger.safeProperties

internal object ObjectDataTypeConverter {

  fun convert(
    schema: Schema<*>,
    convert: (Schema<*>, String) -> Result<DataType<out Any>> ): Result<ObjectDataType> {

    val allowAdditionalProperties = (schema.additionalProperties as? Boolean?) != false
    val additionalPropertiesSchema = schema.additionalProperties.takeIf { it !is Boolean } as Schema<*>?
    val additionalPropertiesDataTypeResult =
      if (additionalPropertiesSchema == null)
        success(null)
      else
        convert(additionalPropertiesSchema, "additionalProperties")

    return result {
      val properties = schema
        .safeProperties()
        .accumulate { (name, subSchema) -> convert(subSchema, name).forProperty(name) }
        .bind()
      val additionalPropertiesDataType = additionalPropertiesDataTypeResult.bind()
      val readOnlyProps = schema.safeProperties().filter { (_, propSchema) -> propSchema.readOnly == true }.keys
      val writeOnlyProps = schema.safeProperties().filter { (_, propSchema) -> propSchema.writeOnly == true }.keys

      ObjectDataType.create(
        name = schema.name,
        properties = properties,
        requiredProperties = schema.required?.toSet() ?: emptySet(),
        readOnlyProperties = readOnlyProps,
        writeOnlyProperties = writeOnlyProps,
        allowAdditionalProperties = allowAdditionalProperties,
        additionalPropertiesDataType = additionalPropertiesDataType,
        isNullable = schema.safeNullable(),
        enum = schema.safeEnum().map { it.normalize() },
        minProperties = schema.minProperties,
        maxProperties = schema.maxProperties
      ).bind()
    }
  }

  fun convertSiblingObject(schema: ComposedSchema,
                           convert: (Schema<*>, String) -> Result<DataType<out Any>>): Result<DataType<out Any>>? {
    return if (schema.properties != null || schema.required != null || schema.additionalProperties != null)
      convert(schema, convert)
    else
      null
  }
}
