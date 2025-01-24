package dev.blitzcraft.contracts.core.datatype

import dev.blitzcraft.contracts.core.Result.Companion.success
import java.math.BigDecimal
import kotlin.random.Random

class NumberDataType(name: String = "Inline 'number' Schema", isNullable: Boolean = false):
    DataType<Number>(name, "number", isNullable, Number::class.java) {

  override fun doValidate(value: Number) = success(value)

  override fun randomValue() = BigDecimal.valueOf(Random.nextDouble(-1_000.0, 1_000.0))
}