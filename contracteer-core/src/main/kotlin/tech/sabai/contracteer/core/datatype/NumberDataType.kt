package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import java.math.BigDecimal

class NumberDataType private constructor(name: String,
                                         isNullable: Boolean,
                                         val range: Range,
                                         allowedValues: AllowedValues? = null):
    DataType<BigDecimal>(name, "number", isNullable, BigDecimal::class.java, allowedValues) {

  override fun doValidate(value: BigDecimal) = range.contains(value)

  override fun doRandomValue(): BigDecimal = range.randomIntegerValue()

  companion object {
    fun create(
      name: String = "Inline 'number' Schema",
      isNullable: Boolean = false,
      enum: List<BigDecimal?> = emptyList(),
      minimum: BigDecimal? = null,
      maximum: BigDecimal? = null,
      exclusiveMinimum: Boolean = false,
      exclusiveMaximum: Boolean = false
    ): Result<NumberDataType> =
      Range.create(minimum, maximum, exclusiveMinimum, exclusiveMaximum)
        .flatMap { range ->
          when {
            !range!!.containsIntegers() -> failure("minimum: '$minimum', maximum: '$maximum', exclusiveMinimum: '$exclusiveMinimum' and exclusiveMaximum: '$exclusiveMaximum' do not allow integer value.")
            enum.isEmpty()              -> success(NumberDataType(name, isNullable, range))
            else                        ->
              AllowedValues
                .create(enum, NumberDataType(name, isNullable, range))
                .map { allowedValues -> NumberDataType(name, isNullable, range, allowedValues) }
          }
        }.mapErrors { "schema '$name': $it" }
  }
}