package tech.sabai.contracteer.core.swagger.converter

import io.swagger.v3.oas.models.media.DateTimeSchema
import tech.sabai.contracteer.core.datatype.DateTimeDataType
import tech.sabai.contracteer.core.swagger.safeEnum
import tech.sabai.contracteer.core.swagger.safeNullable
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME

object DateTimeSchemaConverter {
  fun convert(schema: DateTimeSchema) =
    DateTimeDataType.create(
      name = schema.name,
      isNullable = schema.safeNullable(),
      enum = schema.safeEnum().map { it?.format(ISO_OFFSET_DATE_TIME) }
    )
}