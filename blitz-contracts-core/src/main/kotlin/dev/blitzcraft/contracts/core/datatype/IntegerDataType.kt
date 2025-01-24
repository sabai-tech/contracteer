package dev.blitzcraft.contracts.core.datatype

import dev.blitzcraft.contracts.core.Result.Companion.failure
import dev.blitzcraft.contracts.core.Result.Companion.success
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.random.Random

class IntegerDataType(
  name: String = "Inline 'integer' Schema",
  isNullable: Boolean = false): DataType<Number>(name, "integer", isNullable, Number::class.java) {

  override fun doValidate(value: Number) =
    when {
      value is Double && value.toBigDecimal().isInteger().not() -> failure("not a valid integer.")
      value is Float && value.toBigDecimal().isInteger().not()  -> failure("not a valid integer.")
      value is BigDecimal && value.isInteger().not()            -> failure("not a valid integer.")
      else                                                      -> success(value)
    }

  override fun randomValue() = BigInteger.valueOf(Random.nextLong(-1_000, 1_000))

  private fun BigDecimal.isInteger() = stripTrailingZeros().scale() <= 0
}