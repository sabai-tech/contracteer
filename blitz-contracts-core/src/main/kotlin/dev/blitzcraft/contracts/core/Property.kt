package dev.blitzcraft.contracts.core

import dev.blitzcraft.contracts.core.datatype.DataType

data class Property(
  val name: String,
  val dataType: DataType<Any>,
  val example: Example? = null,
  val required: Boolean = false) {

  fun value(): Any? = if (example != null) example.value else dataType.nextValue()

  fun stringValue(): String? = value()?.toString() // manage complex type like array and object

  fun validateValue(value: Any?) = when {
    value == null -> SimpleValidationResult()
    else          -> dataType.validateValue(value).forProperty(name)
  }

  fun parseAndValidate(stringValue: String?) = when {
    stringValue == null -> SimpleValidationResult()
    else                -> dataType.parseAndValidate(stringValue).forProperty(name)
  }
}
