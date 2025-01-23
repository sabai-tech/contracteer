package dev.blitzcraft.contracts.core.datatype

import dev.blitzcraft.contracts.core.Result
import dev.blitzcraft.contracts.core.accumulate

@Suppress("UNCHECKED_CAST")
class ArrayDataType(
  name: String = "Inline Schema",
  val itemDataType: DataType<out Any>,
  isNullable: Boolean = false):
    DataType<Array<Any?>>(name, "array", isNullable, Array::class.java as Class<Array<Any?>>) {

  override fun doValidate(value: Array<Any?>): Result<Array<Any?>> =
    value.accumulate { index, itemValue -> itemDataType.validate(itemValue).forIndex(index) }.map { value }

  override fun randomValue(): Array<Any?> =
    Array((1..5).random()) { itemDataType.randomValue() }
}