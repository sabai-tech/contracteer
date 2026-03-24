package tech.sabai.contracteer.core.swagger.datatype

import io.github.oshai.kotlinlogging.KotlinLogging
import tech.sabai.contracteer.core.Result
import io.swagger.v3.oas.models.media.DateTimeSchema
import tech.sabai.contracteer.core.datatype.DateTimeDataType
import tech.sabai.contracteer.core.swagger.safeEnum
import tech.sabai.contracteer.core.swagger.safeNullable
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME

internal object DateTimeDataTypeConverter {
  private val logger = KotlinLogging.logger {}

  fun convert(schema: DateTimeSchema): Result<DateTimeDataType> {
    if (schema.pattern != null) logger.warn { "Schema '${schema.name}': 'pattern' ignored because 'format: date-time' takes precedence." }
    if (schema.minLength != null || schema.maxLength != null) logger.warn { "Schema '${schema.name}': 'minLength'/'maxLength' ignored because 'format: date-time' takes precedence." }
    
    return DateTimeDataType.create(
      name = schema.name,
      isNullable = schema.safeNullable(),
      enum = schema.safeEnum().map { it?.format(ISO_OFFSET_DATE_TIME) }
    )
  }
}