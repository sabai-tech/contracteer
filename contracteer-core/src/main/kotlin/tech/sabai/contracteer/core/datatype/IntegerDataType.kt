package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import java.math.BigDecimal

class IntegerDataType private constructor(name: String,
                                          isNullable: Boolean,
                                          val range: Range,
                                          allowedValues: AllowedValues? = null):
    DataType<BigDecimal>(name, "integer", isNullable, BigDecimal::class.java, allowedValues) {

  override fun isFullyStructured() = false

  override fun doValidate(value: BigDecimal) =
    if (!value.isInteger()) failure("The provided value is not an integer.")
    else range.contains(value)


  override fun doRandomValue(): BigDecimal = range.randomIntegerValue()

  companion object {
    fun create(
      name: String,
      isNullable: Boolean = false,
      enum: List<BigDecimal?> = emptyList(),
      minimum: BigDecimal? = null,
      maximum: BigDecimal? = null,
      exclusiveMinimum: Boolean = false,
      exclusiveMaximum: Boolean = false
    ): Result<IntegerDataType> =
      Range.create(minimum, maximum, exclusiveMinimum, exclusiveMaximum)
        .flatMap { range ->
          when {
            minimum != null && !minimum.isInteger() -> failure("minimum must be an integer.")
            maximum != null && !maximum.isInteger() -> failure("maximum must be an integer.")
            enum.isEmpty()              -> success(IntegerDataType(name, isNullable, range!!))
            else                        ->
              AllowedValues
                .create(enum, IntegerDataType(name, isNullable, range!!))
                .map { allowedValues -> IntegerDataType(name, isNullable, range, allowedValues) }
          }
        }
  }

}

private fun BigDecimal.isInteger() = stripTrailingZeros().scale() <= 0

