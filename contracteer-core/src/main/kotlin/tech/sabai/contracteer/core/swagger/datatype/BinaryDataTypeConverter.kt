package tech.sabai.contracteer.core.swagger.datatype

import io.github.oshai.kotlinlogging.KotlinLogging
import tech.sabai.contracteer.core.Result
import io.swagger.v3.oas.models.media.BinarySchema
import tech.sabai.contracteer.core.datatype.BinaryDataType
import tech.sabai.contracteer.core.swagger.safeEnum
import tech.sabai.contracteer.core.swagger.safeNullable

internal object BinaryDataTypeConverter {
  private val logger = KotlinLogging.logger {}

  fun convert(schema: BinarySchema): Result<BinaryDataType> {
    if (schema.pattern != null) logger.warn { "Schema '${schema.name}': 'pattern' ignored because 'format: binary' takes precedence." }
    
    return BinaryDataType.create(
      name = schema.name,
      isNullable = schema.safeNullable(),
      minLength = schema.minLength,
      maxLength = schema.maxLength,
      enum = schema.safeEnum().map { String(it) }
    )
  }
}