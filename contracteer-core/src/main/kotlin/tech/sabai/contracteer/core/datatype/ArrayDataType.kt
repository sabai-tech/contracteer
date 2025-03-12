package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.accumulateWithIndex

class ArrayDataType private constructor(name: String,
                                        val itemDataType: DataType<out Any>,
                                        isNullable: Boolean,
                                        allowedValues: AllowedValues? = null):
    DataType<List<Any?>>(name, "array", isNullable, List::class.java, allowedValues) {

  override fun isFullyStructured() = false

  override fun doValidate(value: List<Any?>): Result<List<Any?>> =
    value.accumulateWithIndex { index, itemValue -> itemDataType.validate(itemValue).forIndex(index) }.map { value }

  override fun doRandomValue(): List<Any?> =
    List((1..5).random()) { itemDataType.randomValue() }

  companion object {
    fun create(name: String = "Inline 'array' Schema",
               itemDataType: DataType<out Any>,
               isNullable: Boolean = false,
               enum: List<Any?> = emptyList()) =
      ArrayDataType(name, itemDataType, isNullable).let { dataType ->
        if (enum.isEmpty()) success(dataType)
        else AllowedValues.create(enum, dataType).map { ArrayDataType(name, itemDataType, isNullable, it) }
      }
  }
}