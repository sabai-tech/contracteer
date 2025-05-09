package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import java.util.UUID.fromString
import java.util.UUID.randomUUID

class UuidDataType private constructor(name: String,
                                       isNullable: Boolean = false,
                                       allowedValues: AllowedValues? = null):
    DataType<String>(name, "string/uuid", isNullable, String::class.java, allowedValues) {

  override fun isFullyStructured() = false

  override fun doValidate(value: String) =
    try {
      fromString(value)
      success(value)
    } catch (_: IllegalArgumentException) {
      failure("the provided string is not a valid UUID")
    }

  override fun doRandomValue() = randomUUID().toString()

  companion object {
    fun create(name: String, isNullable: Boolean, enum: List<String?>) =
      UuidDataType(name, isNullable).let { dataType ->
        if (enum.isEmpty()) success(dataType)
        else AllowedValues.create(enum, dataType).map { UuidDataType(name, isNullable, it) }
      }
  }
}