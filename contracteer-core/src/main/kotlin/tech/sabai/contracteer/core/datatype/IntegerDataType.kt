package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.random.Random

class IntegerDataType private constructor(name: String, isNullable: Boolean, allowedValues: AllowedValues? = null):
    DataType<Number>(name, "integer", isNullable, Number::class.java, allowedValues) {

  override fun doValidate(value: Number) =
    when {
      value is Double && value.toBigDecimal().isInteger().not() -> failure("not a valid integer.")
      value is Float && value.toBigDecimal().isInteger().not()  -> failure("not a valid integer.")
      value is BigDecimal && value.isInteger().not()            -> failure("not a valid integer.")
      else                                                      -> success(value)
    }

  override fun doRandomValue(): BigInteger = BigInteger.valueOf(Random.nextLong(-1_000, 1_000))

  private fun BigDecimal.isInteger() = stripTrailingZeros().scale() <= 0

  companion object {
    fun create(
      name: String = "Inline 'integer' Schema",
      isNullable: Boolean = false,
      enum: List<Any?> = emptyList()
    ) =
      IntegerDataType(name, isNullable).let { dataType ->
        if (enum.isEmpty()) success(dataType)
        else AllowedValues.create(enum, dataType).map { IntegerDataType(name, isNullable, it) }
      }
  }
}

