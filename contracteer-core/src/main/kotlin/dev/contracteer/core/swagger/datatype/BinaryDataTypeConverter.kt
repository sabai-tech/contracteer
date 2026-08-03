package dev.contracteer.core.swagger.datatype

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.models.media.Schema
import dev.contracteer.core.Result
import dev.contracteer.core.Result.Companion.failure
import dev.contracteer.core.Result.Companion.success
import dev.contracteer.core.datatype.BinaryDataType
import dev.contracteer.core.swagger.isNullable
import dev.contracteer.core.swagger.mapEnum

internal object BinaryDataTypeConverter {
  private val logger = KotlinLogging.logger {}

  fun convert(schema: Schema<*>): Result<BinaryDataType> {
    if (schema.pattern != null) logger.warn { "Schema '${schema.name}': 'pattern' ignored because 'format: binary' takes precedence." }

    return schema
      .mapEnum {
        when (it) {
          is String    -> success(it)
          is ByteArray -> success(String(it))
          else         -> failure("Schema '${schema.name}': enum value '$it' is not a valid binary string")
        }
      }.flatMap { enum ->
        BinaryDataType.create(
          name = schema.name,
          isNullable = schema.isNullable(),
          minLength = schema.minLength,
          maxLength = schema.maxLength,
          enum = enum
        )
      }
  }
}