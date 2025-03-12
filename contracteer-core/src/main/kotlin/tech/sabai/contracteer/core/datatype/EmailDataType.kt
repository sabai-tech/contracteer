package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success

class EmailDataType private constructor(name: String,
                                        isNullable: Boolean,
                                        val lengthRange: Range,
                                        allowedValues: AllowedValues? = null):
    DataType<String>(name, "string/email", isNullable, String::class.java, allowedValues) {

  private val candidateChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"

  private val emailRegex =
    ("^(?:[a-zA-Z0-9!#\$%&'*+/=?^_`{|}~-]+(?:\\.[a-zA-Z0-9!#\$%&'*+/=?^_`{|}~-]+)*|" +
     "\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|" +
     "\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@" +
     "(?:(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}|" +
     "\\[(?:(?:25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2})\\.){3}" +
     "(?:25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2}|[a-zA-Z0-9-]*[a-zA-Z0-9]:" +
     "(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|" +
     "\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+))\$").toRegex()

  override fun isFullyStructured() = false

  override fun doValidate(value: String): Result<String> =
    lengthRange.contains(value.length.toBigDecimal()).let { result ->
      when {
        result.isFailure()        -> result.mapErrors { "Invalid string length: ${value.length}. Expected length within $lengthRange." }
        emailRegex.matches(value) -> success(value)
        else                      -> failure<Any>("not a valid email")
      }.retypeError()
    }

  override fun doRandomValue(): String {
    val length = lengthRange.randomIntegerValue().toLong().coerceAtMost(50)
    val randomString = (1..length).map { candidateChars.random() }.joinToString("")
    return randomString.toCharArray().also {
      it[randomString.length - 3] = '.'
      it[(randomString.length - 3) / 2] = '@'
    }.joinToString("")
  }

  companion object {
    fun create(
      name: String = "Inline 'string/email' Schema",
      isNullable: Boolean = false,
      minLength: Int? = 6,
      maxLength: Int? = null,
      enum: List<String?> = emptyList(),
    ) =
      when {
        (minLength != null && minLength < 6) || (maxLength != null && maxLength < 6) -> failure("schema '$name': 'minLength' and 'maxLength' must be at least 6 to form a valid email.")
        else                                                                         ->
          Range.create((minLength ?: 6).toBigDecimal(), maxLength?.toBigDecimal())
            .flatMap { range ->
              val dataType = EmailDataType(name, isNullable, range!!)

              if (enum.isEmpty()) success(dataType)
              else AllowedValues.create(enum, dataType).map { EmailDataType(name, isNullable, range, it) }
            }
            .mapErrors { "schema '$name': $it" }
      }
  }
}