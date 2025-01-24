package dev.blitzcraft.contracts.core.datatype

import dev.blitzcraft.contracts.core.Result.Companion.failure
import dev.blitzcraft.contracts.core.Result.Companion.success
import java.util.UUID.fromString
import java.util.UUID.randomUUID

class UuidDataType(name: String = "Inline 'string/uuid' Schema", isNullable: Boolean = false):
    DataType<String>(name, "string/uuid", isNullable, String::class.java) {

  override fun doValidate(value: String) =
    try {
      fromString(value)
      success(value)
    } catch (e: IllegalArgumentException) {
      failure("not a valid UUID")
    }

  override fun randomValue() = randomUUID().toString()
}