package dev.blitzcraft.contracts.core.datatype

import dev.blitzcraft.contracts.core.validation.ValidationResult
import dev.blitzcraft.contracts.core.validation.ValidationResult.Companion.error
import dev.blitzcraft.contracts.core.validation.ValidationResult.Companion.success
import java.util.*

class Base64DataType(name: String = "Inline Schema", isNullable: Boolean = false):
    DataType<String>(name, "string/byte", isNullable, String::class.java) {

  private val loremIpsum =
    "Lorem ipsum dolor sit amet consectetur adipiscing elit sed do eiusmod tempor incididunt ut labore et dolore magna aliqua"


  override fun doValidate(value: String): ValidationResult {
    return try {
      Base64.getDecoder().decode(value)
      success()
    } catch (e: IllegalArgumentException) {
      error("not a valid Base64 encoded string")
    }
  }

  override fun randomValue() =
    Base64.getEncoder().encodeToString(loremIpsum.split(" ").random().toByteArray())

}