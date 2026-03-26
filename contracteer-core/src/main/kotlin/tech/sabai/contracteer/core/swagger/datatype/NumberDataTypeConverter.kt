package tech.sabai.contracteer.core.swagger.datatype

import io.swagger.v3.oas.models.media.Schema
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.datatype.NumberDataType
import tech.sabai.contracteer.core.datatype.Range
import tech.sabai.contracteer.core.swagger.safeEnum
import tech.sabai.contracteer.core.swagger.safeExclusiveMaximum
import tech.sabai.contracteer.core.swagger.safeExclusiveMinimum
import tech.sabai.contracteer.core.swagger.safeNullable
import java.math.BigDecimal

internal object NumberDataTypeConverter {
  fun convert(schema: Schema<BigDecimal>): Result<NumberDataType> {
    val formatRangeResult = formatRange(schema.format)
    if (formatRangeResult.isFailure()) return formatRangeResult.retypeError()

    val formatRange = formatRangeResult.value!!
    return when {
      schema.minimum != null && formatRange.contains(schema.minimum).isFailure() ->
        failure("minimum (${schema.minimum}) is out of range for format '${schema.format}'")

      schema.maximum != null && formatRange.contains(schema.maximum).isFailure() ->
        failure("maximum (${schema.maximum}) is out of range for format '${schema.format}'")

      else                                                                       ->
        NumberDataType.create(
          name = schema.name,
          isNullable = schema.safeNullable(),
          minimum = schema.minimum ?: formatRange.minimum,
          maximum = schema.maximum ?: formatRange.maximum,
          exclusiveMinimum = schema.safeExclusiveMinimum(),
          exclusiveMaximum = schema.safeExclusiveMaximum(),
          enum = schema.safeEnum(),
          multipleOf = schema.multipleOf
        )
    }
  }

  private fun formatRange(format: String?): Result<Range> =
    when (format) {
      null     -> Range.create()
      "float"  -> Range.create(Float.MAX_VALUE.toBigDecimal().negate(), Float.MAX_VALUE.toBigDecimal())
      "double" -> Range.create(Double.MAX_VALUE.toBigDecimal().negate(), Double.MAX_VALUE.toBigDecimal())
      else     -> failure("Unsupported format '$format' for number type")
    }
}
