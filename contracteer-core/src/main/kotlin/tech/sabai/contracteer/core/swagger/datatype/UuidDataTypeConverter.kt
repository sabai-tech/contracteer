package tech.sabai.contracteer.core.swagger.datatype

import io.github.oshai.kotlinlogging.KotlinLogging
import tech.sabai.contracteer.core.Result
import io.swagger.v3.oas.models.media.UUIDSchema
import tech.sabai.contracteer.core.datatype.UuidDataType
import tech.sabai.contracteer.core.swagger.safeEnum
import tech.sabai.contracteer.core.swagger.safeNullable

internal object UuidDataTypeConverter {
  private val logger = KotlinLogging.logger {}

  fun convert(schema: UUIDSchema): Result<UuidDataType> {
    if (schema.pattern != null) logger.warn { "Schema '${schema.name}': 'pattern' ignored because 'format: uuid' takes precedence." }
    if (schema.minLength != null || schema.maxLength != null) logger.warn { "Schema '${schema.name}': 'minLength'/'maxLength' ignored because 'format: uuid' takes precedence." }
    return UuidDataType.create(
      name = schema.name,
      isNullable = schema.safeNullable(),
      enum = schema.safeEnum().map { it.toString() }
    )
  }
}