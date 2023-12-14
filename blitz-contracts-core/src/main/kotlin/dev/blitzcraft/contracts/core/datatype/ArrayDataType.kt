package dev.blitzcraft.contracts.core.datatype

import io.swagger.v3.oas.models.media.ArraySchema

data class ArrayDataType(val itemDataType: DataType<Any>): DataType<Array<Any>> {

  constructor(schema: ArraySchema): this(DataType.from(schema.items))

  override fun nextValue(): Array<Any> = Array((1..5).random()) { itemDataType.nextValue() }

  override fun regexPattern(): String {
    throw RuntimeException("String Pattern for Array is not defined")
  }
}