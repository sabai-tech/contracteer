package dev.blitzcraft.contracts.core.datatype

import java.math.BigDecimal
import kotlin.random.Random

class DecimalDataType: DataType<BigDecimal> {
  override fun regexPattern() = "-?(\\d*\\.\\d+)"

  override fun nextValue(): BigDecimal = BigDecimal.valueOf(Random.nextDouble(-1_000.0, 1_000.0))
}