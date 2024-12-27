package dev.blitzcraft.contracts.core.datatype

import dev.blitzcraft.contracts.core.validation.validate

class ArrayDataType(
  name: String = "Inline Schema",
  private val itemDataType: DataType<*>,
  isNullable: Boolean = false): DataType<Array<*>>(name, "array", isNullable, Array::class.java) {

  override fun doValidate(value: Array<*>) =
    value.validate { index, itemValue -> itemDataType.validate(itemValue).forIndex(index) }


  override fun randomValue(): Array<*> = Array((1..5).random()) { itemDataType.randomValue() }
}