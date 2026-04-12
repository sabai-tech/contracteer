package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import java.math.BigDecimal
import java.math.BigDecimal.ONE
import java.math.BigInteger
import java.math.RoundingMode.*
import kotlin.random.Random
import kotlin.random.asJavaRandom

/**
 * A numeric range defined by optional minimum and maximum bounds, with optional exclusivity.
 *
 * Used by numeric and string-length data types to enforce OpenAPI `minimum`, `maximum`,
 * `exclusiveMinimum`, `exclusiveMaximum`, `minLength`, and `maxLength` constraints.
 */
class Range private constructor(
  val minimum: BigDecimal?,
  val maximum: BigDecimal?,
  val exclusiveMinimum: Boolean = false,
  val exclusiveMaximum: Boolean = false
) {

  val isBounded: Boolean = minimum != null && maximum != null

  fun contains(value: BigDecimal): Result<BigDecimal> {
    val lowerOk = when (minimum) {
      null -> true
      else -> if (exclusiveMinimum) value > minimum else value >= minimum
    }
    val upperOk = when (maximum) {
      null -> true
      else -> if (exclusiveMaximum) value < maximum else value <= maximum
    }
    return when {
      lowerOk && upperOk -> success(value)
      else               -> failure("The value is not within the range of $this")
    }
  }

  fun containsIntegers(): Boolean =
    firstValidMultipleIndex(ONE) <= lastValidMultipleIndex(ONE)

  fun randomIntegerValue(): BigDecimal {
    if (!containsIntegers()) throw IllegalArgumentException("Cannot generate random integer for the range $this as it does not contain any integer value.")

    val min = firstValidMultipleIndex(ONE).toBigInteger()
    val max = lastValidMultipleIndex(ONE).toBigInteger()
    return when (min) {
      max  -> min.toBigDecimal()
      else -> randomBigInteger(min, max).toBigDecimal()
    }
  }

  fun randomValue(): BigDecimal {
    val min = effectiveMinimum().max(DEFAULT_WIDTH.negate())
    val max = effectiveMaximum().min(DEFAULT_WIDTH)
    val diff = max.subtract(min)
    val raw = min.add(diff.multiply(BigDecimal.valueOf(Random.nextDouble())))
    return when {
      exclusiveMaximum && raw.compareTo(max) == 0 -> raw.subtract(ONE.scaleByPowerOfTen(-diff.scale()))
      else                                        -> raw
    }.setScale(4, HALF_UP)
  }

  fun containsMultipleOf(multipleOf: BigDecimal): Boolean =
    firstValidMultipleIndex(multipleOf) <= lastValidMultipleIndex(multipleOf)

  fun randomMultipleOf(multipleOf: BigDecimal): BigDecimal {
    val min = firstValidMultipleIndex(multipleOf).toBigInteger()
    val max = lastValidMultipleIndex(multipleOf).toBigInteger()
    return randomBigInteger(min, max).toBigDecimal().multiply(multipleOf)
  }

  private fun firstValidMultipleIndex(multipleOf: BigDecimal): BigDecimal {
    val index = effectiveMinimum().divide(multipleOf, 0, CEILING)
    return when {
      exclusiveMinimum && index.multiply(multipleOf).compareTo(effectiveMinimum()) == 0 -> index.add(ONE)
      else -> index
    }
  }

  private fun lastValidMultipleIndex(multipleOf: BigDecimal): BigDecimal {
    val index = effectiveMaximum().divide(multipleOf, 0, FLOOR)
    return when {
      exclusiveMaximum && index.multiply(multipleOf).compareTo(effectiveMaximum()) == 0 -> index.subtract(ONE)
      else -> index
    }
  }

  private fun randomBigInteger(min: BigInteger, max: BigInteger): BigInteger {
    val range = max - min
    return min + BigInteger(range.bitLength(), Random.asJavaRandom()).mod(range + BigInteger.ONE)
  }

  private fun effectiveMinimum() =
    when {
      minimum != null -> minimum
      maximum != null -> maximum.subtract(DEFAULT_WIDTH)
      else            -> DEFAULT_WIDTH.divide(2.toBigDecimal()).negate()
    }

  private fun effectiveMaximum() =
    when {
      maximum != null -> maximum
      minimum != null -> minimum.add(DEFAULT_WIDTH)
      else            -> DEFAULT_WIDTH.divide(2.toBigDecimal())
    }

  override fun toString(): String {
    val minBound = minimum?.toString() ?: "-∞"
    val maxBound = maximum?.toString() ?: "+∞"
    val minBracket = if (exclusiveMinimum) "(" else "["
    val maxBracket = if (exclusiveMaximum) ")" else "]"
    return "$minBracket$minBound, $maxBound$maxBracket"
  }

  companion object {
    private val DEFAULT_WIDTH = BigDecimal(10_000)

    @JvmStatic
    @JvmOverloads
    fun create(minimum: BigDecimal? = null,
               maximum: BigDecimal? = null,
               exclusiveMinimum: Boolean = false,
               exclusiveMaximum: Boolean = false): Result<Range> {
      return when {
        minimum != null && maximum != null && minimum > maximum                                            -> failure("'minimum' ($minimum) must be less than or equal to 'maximum' ($maximum)")
        minimum != null && maximum != null && minimum == maximum && (exclusiveMinimum || exclusiveMaximum) -> failure("when 'minimum' equals 'maximum', both 'exclusiveMinimum' and 'exclusiveMaximum' must be false")
        else                                                                                               ->
          success(Range(minimum, maximum, exclusiveMinimum, exclusiveMaximum))
      }
    }
  }
}