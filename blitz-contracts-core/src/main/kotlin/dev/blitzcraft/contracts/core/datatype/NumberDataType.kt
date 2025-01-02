package dev.blitzcraft.contracts.core.datatype

import dev.blitzcraft.contracts.core.validation.ValidationResult.Companion.success
import java.math.BigDecimal
import kotlin.random.Random

class NumberDataType(name: String = "Inline Schema", isNullable: Boolean = false):
    DataType<Number>(name, "number", isNullable, Number::class.java) {

  override fun doValidate(value: Number) = success()

  override fun randomValue() = BigDecimal.valueOf(Random.nextDouble(-1_000.0, 1_000.0))
}