package tech.sabai.contracteer.core.datatype

import io.github.oshai.kotlinlogging.KotlinLogging
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.datatype.GenerationOutcome.Value
import java.util.*
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

/** OpenAPI `string` type with `format: byte`. Values must be valid Base64-encoded strings. */
class Base64DataType private constructor(name: String,
                                         isNullable: Boolean,
                                         val lengthRange: Range,
                                         allowedValues: AllowedValues? = null):
    ResolvedDataType<String>(name, "string/byte", isNullable, String::class.java, allowedValues) {

  override fun doValidate(value: String) =
    lengthRange.contains(value.length.toBigDecimal())
      .mapErrors { "Invalid length. Expected a value within $lengthRange, but got ${value.length}." }
      .andThen {
        try {
          Base64.getDecoder().decode(value)
          success(value)
        } catch (e: IllegalArgumentException) {
          logger.debug { e }
          failure("Invalid Base64 encoding. The provided string is not a valid Base64 encoded value.")
        }
      }

  override fun doRandomValue(ctx: GenerationContext): GenerationOutcome<String> {
    val randomBytes = ByteArray(randomByteCount()).also { Random.nextBytes(it) }
    return Value(Base64.getEncoder().encodeToString(randomBytes))
  }

  // Standard Base64 encoding: every 3 input bytes produce 4 Base64 characters.
  // Pick an encoded length aligned to a multiple of 4, then derive a byte count whose
  // encoded form lands on that length.
  private fun randomByteCount(): Int {
    val encodedLength = closestMultipleOf4(lengthRange.randomIntegerValue().toInt()).coerceAtMost(MAX_ENCODED_LENGTH)
    val groups = encodedLength / 4
    val minBytes = (groups - 1) * 3 + 1
    val maxBytes = groups * 3
    return if (maxBytes > minBytes) minBytes + Random.nextInt(maxBytes - minBytes + 1) else minBytes
  }

  private fun closestMultipleOf4(value: Int): Int {
    val remainder = value % 4
    return if (remainder < 2) value - remainder else value + (4 - remainder)
  }

  companion object {
    private const val MAX_ENCODED_LENGTH = 100

    @JvmStatic
    @JvmOverloads
    fun create(
      name: String,
      isNullable: Boolean = false,
      enum: List<String?> = emptyList(),
      minLength: Int? = 4,
      maxLength: Int? = null) =
      when {
        (minLength != null && minLength < 4) || (maxLength != null && maxLength < 4)           -> failure("'minLength' and 'maxLength' must be at least 4 for Base64 encoded strings.")
        (minLength != null && minLength % 4 != 0) || (maxLength != null && maxLength % 4 != 0) -> failure("'minLength' and 'maxLength' must be multiples of 4 for Base64 encoded strings.")
        else                                                                                   ->
          Range.create((minLength ?: 4).toBigDecimal(), maxLength?.toBigDecimal())
            .flatMap { range ->
              val dataType = Base64DataType(name, isNullable, range)
              if (enum.isEmpty()) success(dataType)
              else AllowedValues.create(enum, dataType).map { Base64DataType(name, isNullable, range, it) }
            }
      }
  }
}