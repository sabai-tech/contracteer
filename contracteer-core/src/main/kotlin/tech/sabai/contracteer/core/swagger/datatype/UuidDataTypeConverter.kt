package tech.sabai.contracteer.core.swagger.datatype

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.models.media.Schema
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.datatype.UuidDataType
import tech.sabai.contracteer.core.swagger.isNullable
import tech.sabai.contracteer.core.swagger.mapEnum
import java.util.UUID

internal object UuidDataTypeConverter {
  private val logger = KotlinLogging.logger {}

  fun convert(schema: Schema<*>): Result<UuidDataType> {
    if (schema.pattern != null) logger.warn { "Schema '${schema.name}': 'pattern' ignored because 'format: uuid' takes precedence." }
    if (schema.minLength != null || schema.maxLength != null) logger.warn { "Schema '${schema.name}': 'minLength'/'maxLength' ignored because 'format: uuid' takes precedence." }
    return schema
      .mapEnum {
        when (it) {
          is String -> success(it)
          is UUID   -> success(it.toString())
          else      -> failure("Schema '${schema.name}': enum value '$it' is not a valid UUID string")
        }
      }.flatMap { enum ->
        UuidDataType.create(
          name = schema.name,
          isNullable = schema.isNullable(),
          enum = enum
        )
      }
  }
}