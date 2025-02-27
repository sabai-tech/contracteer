package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import java.util.*
import kotlin.random.Random

class Base64DataType private constructor(name: String,
                                         isNullable: Boolean,
                                         val lengthRange: Range,
                                         allowedValues: AllowedValues? = null):
    DataType<String>(name, "string/byte", isNullable, String::class.java, allowedValues) {

  override fun doValidate(value: String) =
    lengthRange.contains(value.length.toBigDecimal()).let { result ->
      if (result.isFailure()) {
        result
          .mapErrors { "Invalid string length: ${value.length}. Expected length within $lengthRange." }
          .retypeError()
      } else
        try {
          Base64.getDecoder().decode(value)
          success(value)
        } catch (e: IllegalArgumentException) {
          failure("not a valid Base64 encoded string")
        }
    }

  override fun doRandomValue(): String {
    val minEncodedLength = closestMultipleOf4(lengthRange.randomIntegerValue().toInt()).coerceAtMost(100)

    // In standard Base64 encoding, every 3 bytes of binary data turn into 4 Base64 characters
    val n = minEncodedLength / 4
    val minBytes = (n - 1) * 3 + 1
    val maxBytes = n * 3

    val byteCount = if (maxBytes > minBytes) {
      minBytes + Random.nextInt(maxBytes - minBytes + 1)
    } else {
      minBytes
    }
    val randomBytes = ByteArray(byteCount).also { Random.nextBytes(it) }
    return Base64.getEncoder().encodeToString(randomBytes)
  }

  private fun closestMultipleOf4(value: Int) =
    if (value > 4) ((value - 1) / 4) * 4 else 4

  companion object {
    fun create(
      name: String = "Inline 'string/byte' Schema",
      isNullable: Boolean = false,
      enum: List<String?> = emptyList(),
      minLength: Int? = 4,
      maxLength: Int? = null) =
      when {
        (minLength != null && minLength < 4) || (maxLength != null && maxLength < 4)           -> failure("schema '$name': 'minLength' and 'maxLength' must be at least 4 for Base64 encoded strings.")
        (minLength != null && minLength % 4 != 0) || (maxLength != null && maxLength % 4 != 0) -> failure("schema '$name': 'minLength' and 'maxLength' must be multiples of 4 for Base64 encoded strings.")
        else                                                                                   ->
          Range.create((minLength ?: 4).toBigDecimal(), maxLength?.toBigDecimal())
            .flatMap { range ->
              val dataType = Base64DataType(name, isNullable, range!!)
              if (enum.isEmpty()) success(dataType)
              else AllowedValues.create(enum, dataType).map { Base64DataType(name, isNullable, range, it) }
            }
            .mapErrors { "schema '$name': $it" }
      }
  }
}