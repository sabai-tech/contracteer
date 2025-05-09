package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import kotlin.math.absoluteValue

class StringDataType private constructor(name: String,
                                         openApiType: String,
                                         isNullable: Boolean,
                                         val lengthRange: Range,
                                         allowedValues: AllowedValues? = null):
    DataType<String>(name, openApiType, isNullable, String::class.java, allowedValues) {

  private val candidateChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 "

  override fun isFullyStructured() = false

  override fun doValidate(value: String) =
    lengthRange
      .contains(value.length.toBigDecimal())
      .mapErrors { "The value has an invalid length. Expected length within $lengthRange, but got : ${value.length}." }
      .map { value }

  override fun doRandomValue(): String =
    (1..lengthRange.randomIntegerValue().toLong().absoluteValue.coerceAtMost(10))
      .map { candidateChars.random() }
      .joinToString("")

  companion object {
    fun create(
      name: String ,
      openApiType: String,
      isNullable: Boolean = false,
      enum: List<String?> = emptyList(),
      minLength: Int? = 0,
      maxLength: Int? = null) =
      if ((minLength != null && minLength < 0) || (maxLength != null && maxLength < 0))
        failure("'minLength' and 'maxlength' must be  greater than or equal to zero.")
      else
        Range.create(minLength?.toBigDecimal(), maxLength?.toBigDecimal())
          .flatMap { range ->
            val dataType = StringDataType(name, openApiType, isNullable, range!!)
            if (enum.isEmpty()) success(dataType)
            else AllowedValues.create(enum, dataType).map { StringDataType(name, openApiType, isNullable, range, it) }
          }
  }
}