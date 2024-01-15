package dev.blitzcraft.contracts.core.datatype

import dev.blitzcraft.contracts.core.validation.ValidationResult.Companion.success
import java.math.BigDecimal
import kotlin.random.Random

class DecimalDataType(isNullable: Boolean = false):
    DataType<BigDecimal>("decimal", isNullable, BigDecimal::class.java) {
  override fun doValidate(value: BigDecimal) = success()

  override fun randomValue(): BigDecimal = BigDecimal.valueOf(Random.nextDouble(-1_000.0, 1_000.0))
}