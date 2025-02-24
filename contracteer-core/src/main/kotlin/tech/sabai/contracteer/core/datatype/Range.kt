package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import java.math.BigDecimal
import java.math.RoundingMode.*
import kotlin.random.Random

class Range private constructor(
  val minimum: BigDecimal?,
  val maximum: BigDecimal?,
  val exclusiveMinimum: Boolean = false,
  val exclusiveMaximum: Boolean = false
) {

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
      else               -> failure("not in the range of $this")
    }
  }

  fun containsIntegers(): Boolean {
    val intLowerBound = effectiveMinimum().setScale(0, CEILING)
    var intUpperBound = effectiveMaximum().setScale(0, FLOOR)

    if (exclusiveMaximum) {
      intUpperBound = intUpperBound.subtract(BigDecimal.ONE)
    }

    return intLowerBound <= intUpperBound
  }

  fun randomIntegerValue(): BigDecimal {
    if (!containsIntegers()) throw IllegalArgumentException("Cannot generate Random Integer for the range: $this")

    val intMinimum = effectiveMinimum().setScale(0, CEILING)
    var intMaximum = effectiveMaximum().setScale(0, FLOOR)

    if (exclusiveMaximum) {
      intMaximum = intMaximum.subtract(BigDecimal.ONE)
    }

    val minLong = intMinimum.toLong()
    val maxLong = intMaximum.toLong() + 1

    return BigDecimal.valueOf(Random.nextLong(minLong, maxLong))
  }

  fun randomValue(): BigDecimal {
    val diff = effectiveMaximum().subtract(effectiveMinimum())
    val randomFactor = BigDecimal.valueOf(Random.nextDouble())
    var result = effectiveMinimum().add(diff.multiply(randomFactor))

    if (exclusiveMaximum && result.compareTo(effectiveMaximum()) == 0) {
      val epsilon = BigDecimal.ONE.scaleByPowerOfTen(-diff.scale())
      result = result.subtract(epsilon)
    }
    return result.setScale(4, HALF_UP)
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

    fun create(minimum: BigDecimal? = null,
               maximum: BigDecimal? = null,
               exclusiveMinimum: Boolean = false,
               exclusiveMaximum: Boolean = false): Result<Range> {
      return when {
        minimum != null && maximum != null && minimum > maximum                                            -> failure("'minimum' ($minimum) must be less than or equal to 'maximum' ($maximum)")
        minimum != null && maximum != null && minimum == maximum && (exclusiveMinimum || exclusiveMaximum) -> failure("if 'minimum' equals 'maximum', 'exclusiveMinimum' and 'exclusiveMaximum' must be set to false")
        else                                                                                               ->
          success(Range(minimum, maximum, exclusiveMinimum, exclusiveMaximum))
      }
    }
  }
}