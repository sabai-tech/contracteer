package dev.blitzcraft.contracts.core.datatype

import java.math.BigInteger
import kotlin.random.Random

class IntegerDataType: DataType<BigInteger> {
  override fun nextValue(): BigInteger = BigInteger.valueOf(Random.nextLong(-1_000, 1_000))
  override fun regexPattern() = "-?(\\d+)"
}
