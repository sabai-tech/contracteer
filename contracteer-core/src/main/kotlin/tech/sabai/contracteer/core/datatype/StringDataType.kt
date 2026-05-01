package tech.sabai.contracteer.core.datatype

import io.github.oshai.kotlinlogging.KotlinLogging
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.datatype.GenerationOutcome.Value
import tech.sabai.contracteer.core.result

/** OpenAPI `string` type, with optional length constraints and format variants (date, email, uuid, etc.). */
class StringDataType private constructor(name: String,
                                         openApiType: String,
                                         isNullable: Boolean,
                                         val lengthRange: Range,
                                         internal val pattern: StringPattern? = null,
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

  override fun doRandomValue(ctx: GenerationContext): GenerationOutcome<String> =
    Value(pattern?.randomValue() ?: randomString())

  private fun randomString(): String =
    (1..randomLength()).map { CANDIDATE_CHARS.random() }.joinToString("")

  private fun randomLength(): Long {
    val cap = maxOf(DEFAULT_MAX_LENGTH, lengthRange.minimum?.toLong() ?: 0L)
    return lengthRange.randomIntegerValue().toLong().coerceIn(0L, cap)
  }

  companion object {
    private val logger = KotlinLogging.logger {}
    private const val CANDIDATE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 "
    private const val DEFAULT_MAX_LENGTH = 10L

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
