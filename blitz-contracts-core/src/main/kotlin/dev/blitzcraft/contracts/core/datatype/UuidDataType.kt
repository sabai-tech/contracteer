package dev.blitzcraft.contracts.core.datatype

import dev.blitzcraft.contracts.core.validation.ValidationResult
import dev.blitzcraft.contracts.core.validation.ValidationResult.Companion.error
import dev.blitzcraft.contracts.core.validation.ValidationResult.Companion.success
import java.util.UUID.fromString
import java.util.UUID.randomUUID

class UuidDataType(name: String= "Inline Schema", isNullable: Boolean = false):
    DataType<String>(name, "string/uuid", isNullable, String::class.java) {

  override fun doValidate(value: String): ValidationResult {
    return try {
      fromString(value)
      success()
    } catch (e: IllegalArgumentException) {
      error("not a valid UUID")
    }
  }

  override fun randomValue() = randomUUID().toString()
}