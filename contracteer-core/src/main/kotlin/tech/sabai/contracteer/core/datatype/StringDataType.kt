package tech.sabai.contracteer.core.datatype

import com.github.curiousoddman.rgxgen.RgxGen
import io.github.oshai.kotlinlogging.KotlinLogging
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success

/** OpenAPI `string` type, with optional length constraints and format variants (date, email, uuid, etc.). */
class StringDataType private constructor(name: String,
                                         openApiType: String,
                                         isNullable: Boolean,
                                         val lengthRange: Range,
                                         val pattern: String? = null,
                                         allowedValues: AllowedValues? = null):
    DataType<String>(name, openApiType, isNullable, String::class.java, allowedValues) {

  private val candidateChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 "
  private val compiledPattern = pattern?.let { Regex(it) }
  private val patternGenerator = pattern?.let { RgxGen.parse(it) }

  override fun isFullyStructured() = false

  override fun doValidate(value: String): Result<String> =
    when {
      compiledPattern != null && !compiledPattern.containsMatchIn(value) -> failure("Value '$value' does not match pattern '$pattern'.")
      compiledPattern != null                                            -> success(value)
      else                                                               ->
        lengthRange
          .contains(value.length.toBigDecimal())
          .mapErrors { "The value has an invalid length. Expected length within $lengthRange, but got : ${value.length}." }
          .map { value }
    }

  override fun doRandomValue(): String =
    if (patternGenerator != null)
      patternGenerator.generate()
    else
      (1..lengthRange.randomIntegerValue().toLong().coerceIn(0, maxOf(10, lengthRange.minimum?.toLong() ?: 0)))
        .map { candidateChars.random() }
        .joinToString("")

  companion object {
    private val logger = KotlinLogging.logger {}

    @JvmStatic
    @JvmOverloads
    fun create(
      name: String,
      openApiType: String,
      isNullable: Boolean = false,
      enum: List<String?> = emptyList(),
      minLength: Int? = 0,
      maxLength: Int? = null,
      pattern: String? = null): Result<StringDataType> {

      if ((minLength != null && minLength < 0) || (maxLength != null && maxLength < 0))
        return failure("'minLength' and 'maxLength' must be greater than or equal to zero.")

      if (isRegexPatternInvalid(pattern)) return failure("'pattern' is not a valid regular expression (ECMA-262 / Java regex): $pattern")

      if (pattern != null && ((minLength != null && minLength > 0) || maxLength != null))
        logger.warn { "Schema '$name': 'minLength'/'maxLength' ignored because 'pattern' takes precedence." }

      return Range.create(minLength?.toBigDecimal(), maxLength?.toBigDecimal())
        .flatMap { range ->
          val dataType = StringDataType(name, openApiType, isNullable, range!!, pattern)
          if (enum.isEmpty()) success(dataType)
          else AllowedValues
            .create(enum, dataType)
            .map { StringDataType(name, openApiType, isNullable, range, pattern, it) }
        }
    }

    private fun isRegexPatternInvalid(pattern: String?): Boolean {
      if (pattern != null) {
        try {
          Regex(pattern)
        } catch (_: Exception) {
          return true
        }
      }
      return false
    }
  }
}