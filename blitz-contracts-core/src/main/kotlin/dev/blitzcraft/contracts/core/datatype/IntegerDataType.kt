package dev.blitzcraft.contracts.core.datatype

import com.fasterxml.jackson.databind.node.BigIntegerNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.LongNode
import com.fasterxml.jackson.databind.node.ShortNode
import dev.blitzcraft.contracts.core.SimpleValidationResult
import dev.blitzcraft.contracts.core.ValidationResult
import java.math.BigInteger
import kotlin.random.Random

class IntegerDataType: DataType<BigInteger> {
  override fun nextValue(): BigInteger = BigInteger.valueOf(Random.nextLong(-1_000, 1_000))
  override fun regexPattern() = "-?(\\d+)"

  override fun validateValue(value: Any) = when (value) {
    is Short, is Int, is Long, is BigInteger                 -> SimpleValidationResult()
    is ShortNode, is IntNode, is LongNode, is BigIntegerNode -> SimpleValidationResult()
    else                                                     -> SimpleValidationResult("Wrong type. Expected type: Integer")
  }

  override fun parseAndValidate(stringValue: String): ValidationResult {
    return stringValue.toBigIntegerOrNull()?.let { validateValue(it) }
           ?: SimpleValidationResult("Wrong type. Expected type: Integer")
  }
}
