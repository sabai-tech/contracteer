package dev.contracteer.core.datatype

import dev.contracteer.core.Result.Companion.failure
import dev.contracteer.core.Result.Companion.success
import dev.contracteer.core.datatype.GenerationOutcome.Value
import java.util.UUID.fromString
import java.util.UUID.randomUUID

/** OpenAPI `string` type with `format: uuid`. Values must be valid UUIDs. */
class UuidDataType private constructor(name: String,
                                       isNullable: Boolean = false,
                                       allowedValues: AllowedValues? = null):
    ResolvedDataType<String>(name, "string/uuid", isNullable, String::class.java, allowedValues) {

  override fun doValidate(value: String) =
    try {
      fromString(value)
      success(value)
    } catch (_: IllegalArgumentException) {
      failure("the provided string is not a valid UUID")
    }

  override fun doRandomValue(ctx: GenerationContext): GenerationOutcome<String> = Value(randomUUID().toString())

  companion object {
    @JvmStatic
    fun create(name: String, isNullable: Boolean, enum: List<String?>) =
      UuidDataType(name, isNullable).let { dataType ->
        if (enum.isEmpty()) success(dataType)
        else AllowedValues.create(enum, dataType).map { UuidDataType(name, isNullable, it) }
      }
  }
}