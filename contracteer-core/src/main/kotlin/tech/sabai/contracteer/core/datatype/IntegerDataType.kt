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

  override fun doValidate(value: BigDecimal) =
    if (value.isInteger().not()) failure("not a valid integer.")
    else range.contains(value)


  override fun doRandomValue(): BigDecimal = range.randomIntegerValue()

  private fun BigDecimal.isInteger() = stripTrailingZeros().scale() <= 0

  companion object {
    fun create(
      name: String = "Inline 'integer' Schema",
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
            !range!!.containsIntegers() -> failure("minimum: '$minimum', maximum: '$maximum', exclusiveMinimum: '$exclusiveMinimum' and exclusiveMaximum: '$exclusiveMaximum' do not allow integer value.")
            enum.isEmpty()              -> success(IntegerDataType(name, isNullable, range))
            else                        ->
              AllowedValues
                .create(enum, IntegerDataType(name, isNullable, range))
                .map { allowedValues -> IntegerDataType(name, isNullable, range, allowedValues) }
          }
        }.mapErrors { "schema '$name': $it" }
  }
}

