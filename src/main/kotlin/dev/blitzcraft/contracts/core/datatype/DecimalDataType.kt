package dev.blitzcraft.contracts.core.datatype

import net.datafaker.Faker
import java.math.BigDecimal

class DecimalDataType:DataType<BigDecimal> {
  override fun regexPattern() = "-?(\\d*\\.\\d+)"

  override fun nextValue(): BigDecimal = BigDecimal.valueOf(Faker().number().randomDouble(3, -100_000L, 100_000L))
}