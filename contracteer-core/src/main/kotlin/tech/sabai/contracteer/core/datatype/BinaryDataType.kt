package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import kotlin.math.absoluteValue
import kotlin.random.Random

class BinaryDataType private constructor(name: String,
                                         isNullable: Boolean,
                                         val lengthRange: Range,
                                         allowedValues: AllowedValues? = null):
    DataType<String>(name, "string/binary", isNullable, String::class.java, allowedValues) {

  override fun isFullyStructured() = false

  override fun doValidate(value: String) =
    lengthRange
      .contains(value.length.toBigDecimal())
      .mapErrors { "Invalid string length: ${value.length}. Expected length within $lengthRange." }
      .map { value }

  override fun doRandomValue(): String =
    ByteArray(lengthRange.randomIntegerValue().toInt().absoluteValue.coerceAtMost(100)).also { Random.nextBytes(it) }.toString(Charsets.ISO_8859_1)

  companion object {
    fun create(
      name: String,
      isNullable: Boolean = false,
      enum: List<String?> = emptyList(),
      minLength: Int? = 0,
      maxLength: Int? = null,
    ) =
      if ((minLength != null && minLength < 0) || (maxLength != null && maxLength < 0))
        failure("'minLength' and 'maxlength' must be equal or greater than zero.")
      else
        Range.create(minLength?.toBigDecimal(), maxLength?.toBigDecimal())
          .flatMap { range ->
            val dataType = BinaryDataType(name, isNullable, range!!)
            if (enum.isEmpty()) success(dataType)
            else AllowedValues.create(enum, dataType).map { BinaryDataType(name, isNullable, range, it) }
          }
  }
}