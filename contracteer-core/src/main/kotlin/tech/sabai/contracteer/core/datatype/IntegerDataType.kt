package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import java.math.BigDecimal

/** OpenAPI `integer` type, with optional range constraints. Values are represented as [BigDecimal]. */
class IntegerDataType private constructor(name: String,
                                          isNullable: Boolean,
                                          val range: Range,
                                          val multipleOf: BigDecimal?,
                                          allowedValues: AllowedValues? = null):
    DataType<BigDecimal>(name, "integer", isNullable, BigDecimal::class.java, allowedValues) {

  override fun isFullyStructured() = false

  override fun doValidate(value: BigDecimal): Result<BigDecimal> {
    return when {
      !value.isInteger()                                                                ->
        failure("The provided value is not an integer.")

      multipleOf != null && value.remainder(multipleOf).compareTo(BigDecimal.ZERO) != 0 ->
        failure("Value $value is not a multiple of $multipleOf")

      else                                                                              ->
        range.contains(value)
    }
  }

  override fun doRandomValue(): BigDecimal =
    if (multipleOf != null) range.randomMultipleOf(multipleOf) else range.randomIntegerValue()

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
      multipleOf: BigDecimal? = null
    ): Result<IntegerDataType> =
      Range
        .create(minimum, maximum, exclusiveMinimum, exclusiveMaximum)
        .flatMap { range ->
          when {
            minimum != null && !minimum.isInteger()                       -> failure("minimum must be an integer.")
            maximum != null && !maximum.isInteger()                       -> failure("maximum must be an integer.")
            multipleOf != null && !range!!.containsMultipleOf(multipleOf) -> failure("Range $range contains no multiple of $multipleOf")
            enum.isEmpty()                                                -> success(IntegerDataType(name,isNullable,range!!,multipleOf))
            else                                                          ->
              AllowedValues
                .create(enum, IntegerDataType(name, isNullable, range!!, multipleOf))
                .map { allowedValues -> IntegerDataType(name, isNullable, range, multipleOf, allowedValues) }
          }
        }
  }
}

private fun BigDecimal.isInteger() = stripTrailingZeros().scale() <= 0
