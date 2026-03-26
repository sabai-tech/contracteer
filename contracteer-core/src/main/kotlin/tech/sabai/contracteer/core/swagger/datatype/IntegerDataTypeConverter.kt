package tech.sabai.contracteer.core.swagger.datatype

import io.swagger.v3.oas.models.media.IntegerSchema
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.datatype.IntegerDataType
import tech.sabai.contracteer.core.datatype.Range
import tech.sabai.contracteer.core.normalize
import tech.sabai.contracteer.core.swagger.safeEnum
import tech.sabai.contracteer.core.swagger.safeExclusiveMaximum
import tech.sabai.contracteer.core.swagger.safeExclusiveMinimum
import tech.sabai.contracteer.core.swagger.safeNullable
import java.math.BigDecimal

internal object IntegerDataTypeConverter {
  fun convert(schema: IntegerSchema): Result<IntegerDataType> {
    val formatRangeResult = formatRange(schema.format)
    if (formatRangeResult.isFailure()) return formatRangeResult.retypeError()

    val formatRange = formatRangeResult.value!!
    return when {
      schema.minimum != null && formatRange.contains(schema.minimum).isFailure() ->
        failure("minimum (${schema.minimum}) is out of range for format '${schema.format}'")

      schema.maximum != null && formatRange.contains(schema.maximum).isFailure() ->
        failure("maximum (${schema.maximum}) is out of range for format '${schema.format}'")

      else                                                                       ->
        IntegerDataType.create(
          name = schema.name,
          isNullable = schema.safeNullable(),
          minimum = schema.minimum ?: formatRange.minimum,
          maximum = schema.maximum ?: formatRange.maximum,
          exclusiveMinimum = schema.safeExclusiveMinimum(),
          exclusiveMaximum = schema.safeExclusiveMaximum(),
          enum = schema.safeEnum().map { it.normalize() as BigDecimal? },
          multipleOf = schema.multipleOf
        )
    }
  }

  private fun formatRange(format: String?): Result<Range> = when (format) {
    null    -> Range.create()
    "int32" -> Range.create(Int.MIN_VALUE.toBigDecimal(), Int.MAX_VALUE.toBigDecimal())
    "int64" -> Range.create(Long.MIN_VALUE.toBigDecimal(), Long.MAX_VALUE.toBigDecimal())
    else    -> failure("Unsupported format '$format' for integer type")
  }
}
