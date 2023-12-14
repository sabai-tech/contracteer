package dev.blitzcraft.contracts.core

import dev.blitzcraft.contracts.core.datatype.DataType

data class Property(
  val dataType: DataType<Any>,
  val example: Example? = null,
  val required: Boolean = false) {
  fun value(): Any? = if (example != null) example.value else dataType.nextValue()
  fun stringValue(): String? = value()?.toString() // manage complex type like array and object
}
