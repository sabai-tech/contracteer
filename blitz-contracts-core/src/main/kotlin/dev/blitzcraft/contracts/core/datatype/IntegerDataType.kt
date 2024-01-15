package dev.blitzcraft.contracts.core.datatype

import dev.blitzcraft.contracts.core.validation.ValidationResult.Companion.success
import java.math.BigInteger
import kotlin.random.Random

class IntegerDataType(isNullable: Boolean = false):
    DataType<BigInteger>("integer", isNullable, BigInteger::class.java) {

  override fun doValidate(value: BigInteger) = success()

  override fun randomValue(): BigInteger = BigInteger.valueOf(Random.nextLong(-1_000, 1_000))
}