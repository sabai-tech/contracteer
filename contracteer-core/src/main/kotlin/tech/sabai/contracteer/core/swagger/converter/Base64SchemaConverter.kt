package tech.sabai.contracteer.core.swagger.converter

import io.swagger.v3.oas.models.media.ByteArraySchema
import tech.sabai.contracteer.core.datatype.Base64DataType
import tech.sabai.contracteer.core.swagger.safeEnum
import tech.sabai.contracteer.core.swagger.safeNullable
import java.util.*

internal object Base64SchemaConverter {
  fun convert(schema: ByteArraySchema) =
    Base64DataType.create(
      name = schema.name,
      isNullable = schema.safeNullable(),
      minLength = schema.minLength,
      maxLength = schema.maxLength,
      enum = schema.safeEnum().map { Base64.getEncoder().encodeToString(it) }
    )
}