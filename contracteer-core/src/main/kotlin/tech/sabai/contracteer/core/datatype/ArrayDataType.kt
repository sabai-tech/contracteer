package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.accumulate

@Suppress("UNCHECKED_CAST")
class ArrayDataType(
  name: String = "Inline 'array' Schema",
  val itemDataType: DataType<out Any>,
  isNullable: Boolean = false):
    DataType<Array<Any?>>(name, "array", isNullable, Array::class.java as Class<Array<Any?>>) {

  override fun doValidate(value: Array<Any?>): Result<Array<Any?>> =
    value.accumulate { index, itemValue -> itemDataType.validate(itemValue).forIndex(index) }.map { value }

  override fun randomValue(): Array<Any?> =
    Array((1..5).random()) { itemDataType.randomValue() }
}