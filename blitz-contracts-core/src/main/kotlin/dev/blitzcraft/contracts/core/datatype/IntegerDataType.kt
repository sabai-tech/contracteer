package dev.blitzcraft.contracts.core.datatype

import net.datafaker.Faker
import java.math.BigInteger

class IntegerDataType: DataType<BigInteger> {
  override fun nextValue(): BigInteger = BigInteger.valueOf(Faker().number().numberBetween(-100_000L, 100_000L))
  override fun regexPattern() = "-?(\\d+)"
}
