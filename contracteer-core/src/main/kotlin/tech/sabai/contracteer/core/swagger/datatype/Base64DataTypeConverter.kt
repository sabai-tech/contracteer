package tech.sabai.contracteer.core.swagger.datatype

import io.github.oshai.kotlinlogging.KotlinLogging
import tech.sabai.contracteer.core.Result
import io.swagger.v3.oas.models.media.ByteArraySchema
import tech.sabai.contracteer.core.datatype.Base64DataType
import tech.sabai.contracteer.core.swagger.safeEnum
import tech.sabai.contracteer.core.swagger.safeNullable
import java.util.*

internal object Base64DataTypeConverter {
  private val logger = KotlinLogging.logger {}

  fun convert(schema: ByteArraySchema): Result<Base64DataType> {
    if (schema.pattern != null) logger.warn { "Schema '${schema.name}': 'pattern' ignored because 'format: byte' takes precedence." }
    
    return Base64DataType.create(
      name = schema.name,
      isNullable = schema.safeNullable(),
      minLength = schema.minLength,
      maxLength = schema.maxLength,
      enum = schema.safeEnum().map { Base64.getEncoder().encodeToString(it) }
    )
  }
}