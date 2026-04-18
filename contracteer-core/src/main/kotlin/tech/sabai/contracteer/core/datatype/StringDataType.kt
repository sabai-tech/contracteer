package tech.sabai.contracteer.core.datatype

import io.github.oshai.kotlinlogging.KotlinLogging
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.result

/** OpenAPI `string` type, with optional length constraints and format variants (date, email, uuid, etc.). */
class StringDataType private constructor(name: String,
                                         openApiType: String,
                                         isNullable: Boolean,
                                         val lengthRange: Range,
                                         private val pattern: StringPattern? = null,
                                         allowedValues: AllowedValues? = null):
    ResolvedDataType<String>(name, openApiType, isNullable, String::class.java, allowedValues) {

  override fun isFullyStructured() = false

  override fun doValidate(value: String): Result<String> =
    when {
      pattern != null -> pattern.validate(value)
      else            ->
        lengthRange
          .contains(value.length.toBigDecimal())
          .mapErrors { "The value has an invalid length. Expected length within $lengthRange, but got : ${value.length}." }
          .map { value }
    }

  override fun doRandomValue(): String =
    if (pattern != null) pattern.randomValue()
    else
      (1..lengthRange.randomIntegerValue().toLong().coerceIn(0, maxOf(10, lengthRange.minimum?.toLong() ?: 0)))
        .map { CANDIDATE_CHARS.random() }
        .joinToString("")

  companion object {
    private val logger = KotlinLogging.logger {}
    private const val CANDIDATE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 "

    @JvmStatic
    @JvmOverloads
    fun create(name: String,
               openApiType: String,
               isNullable: Boolean = false,
               enum: List<String?> = emptyList(),
               minLength: Int? = 0,
               maxLength: Int? = null,
               pattern: String? = null): Result<StringDataType> {

      if ((minLength != null && minLength < 0) || (maxLength != null && maxLength < 0))
        return failure("'minLength' and 'maxLength' must be greater than or equal to zero.")

      return result {
        val stringPattern = parsePattern(pattern).bind()
        warnIfLengthIgnored(name, stringPattern, minLength, maxLength)
        val range = Range.create(minLength?.toBigDecimal(), maxLength?.toBigDecimal()).bind()
        buildDataType(name, openApiType, isNullable, range, stringPattern, enum).bind()
      }
    }

    private fun parsePattern(pattern: String?): Result<StringPattern?> =
      if (pattern == null) success(null) else StringPattern.create(pattern)

    private fun warnIfLengthIgnored(name: String, pattern: StringPattern?, minLength: Int?, maxLength: Int?) {
      if (pattern != null && ((minLength != null && minLength > 0) || maxLength != null))
        logger.warn { "Schema '$name': 'minLength'/'maxLength' ignored because 'pattern' takes precedence." }
    }

    private fun buildDataType(name: String,
                              openApiType: String,
                              isNullable: Boolean,
                              range: Range,
                              stringPattern: StringPattern?,
                              enum: List<String?>): Result<StringDataType> {
      val dataType = StringDataType(name, openApiType, isNullable, range, stringPattern)
      return if (enum.isEmpty())
        success(dataType)
      else AllowedValues
        .create(enum, dataType)
        .map { StringDataType(name, openApiType, isNullable, range, stringPattern, it) }
    }
  }
}
