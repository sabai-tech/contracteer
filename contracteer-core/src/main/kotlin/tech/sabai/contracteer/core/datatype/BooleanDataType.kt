package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result.Companion.success
import kotlin.random.Random

class BooleanDataType private constructor(name: String, isNullable: Boolean, allowedValues: AllowedValues? = null):
    DataType<Boolean>(name, "boolean", isNullable, Boolean::class.javaObjectType, allowedValues) {

  override fun doValidate(value: Boolean) = success(value)

  override fun doRandomValue() = Random.nextBoolean()

  companion object {
    fun create(
      name: String = "Inline 'boolean' Schema",
      isNullable: Boolean = false,
      enum: List<Boolean?> = emptyList()
    ) =
      BooleanDataType(name, isNullable).let { dataType ->
        if (enum.isEmpty()) success(dataType)
        else AllowedValues.create(enum, dataType).map { BooleanDataType(name, isNullable, it) }
      }
  }
}