package dev.blitzcraft.contracts.core.datatype

import com.fasterxml.jackson.databind.node.DoubleNode
import com.fasterxml.jackson.databind.node.FloatNode
import dev.blitzcraft.contracts.core.SimpleValidationResult
import java.math.BigDecimal
import kotlin.random.Random

class DecimalDataType: DataType<BigDecimal> {
  override fun regexPattern() = "-?(\\d*\\.\\d+)"

  override fun nextValue(): BigDecimal = BigDecimal.valueOf(Random.nextDouble(-1_000.0, 1_000.0))
  override fun validateValue(value: Any) = when (value) {
    is Float, is Double, is BigDecimal -> SimpleValidationResult()
    is FloatNode, is DoubleNode        -> SimpleValidationResult()
    else                               -> SimpleValidationResult("Wrong type. Expected type: Decimal")
  }

  override fun parseAndValidate(stringValue: String) =
    stringValue.toBigDecimalOrNull()?.let { validateValue(it) }
    ?: SimpleValidationResult("Wrong type. Expected type: Decimal")
}