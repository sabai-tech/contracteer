package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.success
import kotlin.random.Random

class BinaryDataType private constructor(name: String, isNullable: Boolean, allowedValues: AllowedValues? = null):
    DataType<String>(name, "string/binary", isNullable, String::class.java, allowedValues) {

  override fun doValidate(value: String): Result<String> =
    success(value)

  override fun doRandomValue(): String =
    ByteArray(100).also { Random.nextBytes(it) }.toString(Charsets.ISO_8859_1)

  companion object {
    fun create(
      name: String = "Inline 'string/binary' Schema",
      isNullable: Boolean = false,
      enum: List<Any?> = emptyList()
    ) =
      BinaryDataType(name, isNullable).let { dataType ->
        if (enum.isEmpty()) success(dataType)
        else AllowedValues.create(enum, dataType).map { BinaryDataType(name, isNullable, it) }
      }
  }
}