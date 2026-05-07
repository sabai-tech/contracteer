package tech.sabai.contracteer.core.swagger.datatype

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.models.media.Schema
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.datatype.NumberDataType
import tech.sabai.contracteer.core.datatype.Range
import tech.sabai.contracteer.core.normalize
import tech.sabai.contracteer.core.swagger.effectiveExclusiveMaximum
import tech.sabai.contracteer.core.swagger.effectiveExclusiveMinimum
import tech.sabai.contracteer.core.swagger.effectiveMaximum
import tech.sabai.contracteer.core.swagger.effectiveMinimum
import tech.sabai.contracteer.core.swagger.isNullable
import tech.sabai.contracteer.core.swagger.mapEnum
import java.math.BigDecimal

internal object NumberDataTypeConverter {
  private val logger = KotlinLogging.logger {}

  fun convert(schema: Schema<*>): Result<NumberDataType> =
    formatRange(schema.name, schema.format).flatMap { formatRange ->
      val minimum = schema.effectiveMinimum()
      val maximum = schema.effectiveMaximum()
      when {
        minimum != null && formatRange.contains(minimum).isFailure() ->
          failure("minimum ($minimum) is out of range for format '${schema.format}'")

        maximum != null && formatRange.contains(maximum).isFailure() ->
          failure("maximum ($maximum) is out of range for format '${schema.format}'")

        else                                                          ->
          schema.mapEnum {
            when (val normalized = it.normalize()) {
              is BigDecimal -> success(normalized)
              else          -> failure("Schema '${schema.name}': enum value '$it' is not a valid number")
            }
          }.flatMap { enum ->
            NumberDataType.create(
              name = schema.name,
              isNullable = schema.isNullable(),
              minimum = minimum ?: formatRange.minimum,
              maximum = maximum ?: formatRange.maximum,
              exclusiveMinimum = schema.effectiveExclusiveMinimum(),
              exclusiveMaximum = schema.effectiveExclusiveMaximum(),
              enum = enum,
              multipleOf = schema.multipleOf
            )
          }
      }
    }

  private fun formatRange(schemaName: String, format: String?): Result<Range> =
    when (format) {
      null     -> Range.create()
      "float"  -> Range.create(Float.MAX_VALUE.toBigDecimal().negate(), Float.MAX_VALUE.toBigDecimal())
      "double" -> Range.create(Double.MAX_VALUE.toBigDecimal().negate(), Double.MAX_VALUE.toBigDecimal())
      else     ->
        Range.create()
          .also { logger.warn { "Schema '$schemaName': unknown format '$format' for number type is ignored." } }
    }
}
