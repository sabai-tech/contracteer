package tech.sabai.contracteer.core.swagger.datatype

import io.github.oshai.kotlinlogging.KotlinLogging
import tech.sabai.contracteer.core.Result
import io.swagger.v3.oas.models.media.DateSchema
import tech.sabai.contracteer.core.datatype.DateDataType
import tech.sabai.contracteer.core.swagger.safeEnum
import tech.sabai.contracteer.core.swagger.safeNullable
import java.time.ZoneId
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE

internal object DateDataTypeConverter {
  private val logger = KotlinLogging.logger {}

  fun convert(schema: DateSchema): Result<DateDataType> {
    if (schema.pattern != null) logger.warn { "Schema '${schema.name}': 'pattern' ignored because 'format: date' takes precedence." }
    if (schema.minLength != null || schema.maxLength != null) logger.warn { "Schema '${schema.name}': 'minLength'/'maxLength' ignored because 'format: date' takes precedence." }
    
    return DateDataType.create(
      name = schema.name,
      isNullable = schema.safeNullable(),
      enum = schema
        .safeEnum()
        .map { it?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()?.format(ISO_LOCAL_DATE) }
    )
  }
}