package dev.blitzcraft.contracts.core.datatype

import com.fasterxml.jackson.databind.node.TextNode
import dev.blitzcraft.contracts.core.SimpleValidationResult
import kotlin.random.Random


class StringDataType: DataType<String> {
  private val loremIpsum =
    "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."

  override fun nextValue() = loremIpsum.split(" ").shuffled().take(Random.nextInt(2, 7)).joinToString(separator = " ")

  override fun regexPattern() = "[\\S\\s]+" // ".*" does not work with JsonPath as it always matches

  override fun validateValue(value: Any) = when (value) {
    is String, is TextNode -> SimpleValidationResult()
    else                   -> SimpleValidationResult("Wrong type. Expected type: String")
  }

  override fun parseAndValidate(stringValue: String) = validateValue(stringValue)
}
