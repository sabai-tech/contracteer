package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import java.math.BigDecimal

/** OpenAPI `number` type, with optional range constraints. Values are represented as [BigDecimal]. */
class NumberDataType private constructor(name: String,
                                         isNullable: Boolean,
                                         val range: Range,
                                         val multipleOf: BigDecimal?,
                                         allowedValues: AllowedValues? = null):
    ResolvedDataType<BigDecimal>(name, "number", isNullable, BigDecimal::class.java, allowedValues) {

  override fun isFullyStructured() = false

  override fun doValidate(value: BigDecimal): Result<BigDecimal> {
    return if (multipleOf != null && value.remainder(multipleOf).compareTo(BigDecimal.ZERO) != 0)
      failure("Value $value is not a multiple of $multipleOf")
    else
      range.contains(value)
  }

  override fun doRandomValue(): BigDecimal =
    if (multipleOf != null) range.randomMultipleOf(multipleOf) else range.randomValue()

  companion object {
    @JvmStatic
    @JvmOverloads
    fun create(
      name: String,
      isNullable: Boolean = false,
      enum: List<BigDecimal?> = emptyList(),
      minimum: BigDecimal? = null,
      maximum: BigDecimal? = null,
      exclusiveMinimum: Boolean = false,
      exclusiveMaximum: Boolean = false,
      multipleOf: BigDecimal? = null): Result<NumberDataType> =
      Range
        .create(minimum, maximum, exclusiveMinimum, exclusiveMaximum)
        .flatMap { range ->
          when {
            multipleOf != null && !range.containsMultipleOf(multipleOf) -> failure("Range $range contains no multiple of $multipleOf")
            enum.isEmpty()                                              -> success(NumberDataType(name, isNullable, range, multipleOf))
            else                                                        ->
              AllowedValues
                .create(enum, NumberDataType(name, isNullable, range, multipleOf))
                .map { allowedValues -> NumberDataType(name, isNullable, range, multipleOf, allowedValues) }
          }
        }
  }
}
