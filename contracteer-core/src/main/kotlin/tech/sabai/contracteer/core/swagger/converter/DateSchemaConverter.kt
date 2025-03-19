package tech.sabai.contracteer.core.swagger.converter

import io.swagger.v3.oas.models.media.DateSchema
import tech.sabai.contracteer.core.datatype.DateDataType
import tech.sabai.contracteer.core.swagger.safeEnum
import tech.sabai.contracteer.core.swagger.safeNullable
import java.time.ZoneId
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE

internal object DateSchemaConverter {
  fun convert(schema: DateSchema) =
    DateDataType.create(
      name = schema.name,
      isNullable = schema.safeNullable(),
      enum = schema
        .safeEnum()
        .map { it?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()?.format(ISO_LOCAL_DATE) }
    )
}