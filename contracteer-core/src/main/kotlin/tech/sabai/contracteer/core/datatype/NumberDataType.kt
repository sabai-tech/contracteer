package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result.Companion.success
import java.math.BigDecimal
import kotlin.random.Random

class NumberDataType private constructor(name: String, isNullable: Boolean, allowedValues: AllowedValues? = null):
    DataType<BigDecimal>(name, "number", isNullable, BigDecimal::class.java, allowedValues) {

  override fun doValidate(value: BigDecimal) = success(value)

  override fun doRandomValue(): BigDecimal = BigDecimal.valueOf(Random.nextDouble(-1_000.0, 1_000.0))

  companion object {
    fun create(
      name: String = "Inline 'number' Schema",
      isNullable: Boolean = false,
      enum: List<Any?> = emptyList()
    ) =
      NumberDataType(name, isNullable).let { dataType ->
        if (enum.isEmpty()) success(dataType)
        else AllowedValues.create(enum, dataType).map { NumberDataType(name, isNullable, it) }
      }
  }
}