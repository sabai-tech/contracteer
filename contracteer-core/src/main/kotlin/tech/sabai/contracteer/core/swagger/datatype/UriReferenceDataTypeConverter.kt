package tech.sabai.contracteer.core.swagger.datatype

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.models.media.Schema
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.datatype.UriReferenceDataType
import tech.sabai.contracteer.core.swagger.isNullable
import tech.sabai.contracteer.core.swagger.mapEnum

internal object UriReferenceDataTypeConverter {
  private val logger = KotlinLogging.logger {}

  fun convert(schema: Schema<*>): Result<UriReferenceDataType> {
    if (schema.minLength != null || schema.maxLength != null)
      logger.warn { "Schema '${schema.name}': 'minLength'/'maxLength' ignored because 'format: uri-reference' takes precedence." }
    if (schema.pattern != null)
      logger.warn { "Schema '${schema.name}': 'pattern' ignored because 'format: uri-reference' takes precedence." }

    return schema
      .mapEnum {
        when (it) {
          is String -> success(it)
          else      -> failure("Schema '${schema.name}': enum value '$it' is not a valid uri-reference string")
        }
      }.flatMap { enum ->
        UriReferenceDataType.create(
          name = schema.name,
          isNullable = schema.isNullable(),
          enum = enum,
        )
      }
  }
}