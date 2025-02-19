package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.accumulate

@Suppress("UNCHECKED_CAST")
class ArrayDataType private constructor(name: String,
                                        val itemDataType: DataType<out Any>,
                                        isNullable: Boolean,
                                        allowedValues: AllowedValues? = null):
    DataType<Array<Any?>>(name, "array", isNullable, Array::class.java as Class<Array<Any?>>, allowedValues) {

  override fun doValidate(value: Array<Any?>): Result<Array<Any?>> =
    value.accumulate { index, itemValue -> itemDataType.validate(itemValue).forIndex(index) }.map { value }

  override fun doRandomValue(): Array<Any?> =
    Array((1..5).random()) { itemDataType.randomValue() }

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