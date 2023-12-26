package dev.blitzcraft.contracts.core.datatype

import com.fasterxml.jackson.databind.node.BooleanNode
import dev.blitzcraft.contracts.core.SimpleValidationResult
import kotlin.random.Random

class BooleanDataType: DataType<Boolean> {

  override fun nextValue(): Boolean = Random.nextBoolean()

  override fun validateValue(value: Any) = when (value) {
    is Boolean, is BooleanNode -> SimpleValidationResult()
    else                       -> SimpleValidationResult("Wrong type. Expected type: Boolean")
  }

  override fun parseAndValidate(stringValue: String) =
    stringValue.toBooleanStrictOrNull()?.let { validateValue(it) }
    ?: SimpleValidationResult("Wrong type. Expected type: Boolean")

  override fun regexPattern(): String = "(true|false)"
}