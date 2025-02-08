package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.success
import kotlin.random.Random

class BinaryDataType(
  name: String = "Inline 'string/binary' Schema",
  isNullable: Boolean = false
): DataType<String>(name, "string/binary", isNullable, String::class.java) {

  override fun doValidate(value: String): Result<String> =
    success(value)

  override fun randomValue(): String =
    ByteArray(100).also { Random.nextBytes(it) }.toString(Charsets.ISO_8859_1)
}