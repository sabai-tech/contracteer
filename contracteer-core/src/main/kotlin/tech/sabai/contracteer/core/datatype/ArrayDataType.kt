package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.accumulateWithIndex
import java.math.BigDecimal
import java.math.RoundingMode.CEILING
import java.math.RoundingMode.FLOOR

/** OpenAPI `array` type, containing items of a single [itemDataType]. */
class ArrayDataType private constructor(name: String,
                                        val itemDataType: DataType<out Any>,
                                        isNullable: Boolean,
                                        val minItems: Int?,
                                        val maxItems: Int?,
                                        val uniqueItems: Boolean,
                                        allowedValues: AllowedValues? = null):
    DataType<List<Any?>>(name, "array", isNullable, List::class.java, allowedValues) {

  override fun isFullyStructured() = false

  override fun asRequestType(): DataType<List<Any?>> =
    itemDataType.asRequestType().let {
      if (it === itemDataType) this
      else ArrayDataType(name, it, isNullable, minItems, maxItems, uniqueItems, allowedValues)
    }

  override fun asResponseType(): DataType<List<Any?>> =
    itemDataType.asResponseType().let {
      if (it === itemDataType) this
      else ArrayDataType(name, it, isNullable, minItems, maxItems, uniqueItems, allowedValues)
    }

  override fun doValidate(value: List<Any?>): Result<List<Any?>> =
    validateConstraints(value) andThen {
      value
        .accumulateWithIndex { index, itemValue -> itemDataType.validate(itemValue).forIndex(index) }
        .map { value }
    }

  private fun validateConstraints(value: List<Any?>): Result<List<Any?>> = when {
    minItems != null && value.size < minItems          -> failure("Array has ${value.size} items but minItems is $minItems")
    maxItems != null && value.size > maxItems          -> failure("Array has ${value.size} items but maxItems is $maxItems")
    uniqueItems && value.size != value.distinct().size -> failure("Array contains duplicate items but uniqueItems is true")
    else                                               -> success(value)
  }

  override fun doRandomValue(): List<Any?> {
    val min = minItems ?: 1
    val max = maxItems ?: maxOf(min, 2)
    val count = (min..max).random()

    return if (!uniqueItems)
      List(count) { itemDataType.randomValue() }
    else
      (1..MAX_GENERATION_ATTEMPTS)
        .asSequence()
        .map { generateSequence { itemDataType.randomValue() }.distinct().take(count).toList() }
        .firstOrNull { it.size == count }
      ?: error("Failed to generate an array with $count unique items after $MAX_GENERATION_ATTEMPTS attempts")
  }

  companion object {
    private const val MAX_GENERATION_ATTEMPTS = 10

    @JvmStatic
    @JvmOverloads
    fun create(name: String,
               itemDataType: DataType<out Any>,
               isNullable: Boolean = false,
               enum: List<Any?> = emptyList(),
               minItems: Int? = null,
               maxItems: Int? = null,
               uniqueItems: Boolean = false): Result<ArrayDataType> {
      if (minItems != null && minItems < 0) return failure("minItems must be non-negative")
      if (maxItems != null && maxItems < 0) return failure("maxItems must be non-negative")
      if (minItems != null && maxItems != null && minItems > maxItems) return failure("minItems ($minItems) must be less than or equal to maxItems ($maxItems)")
      if (uniqueItems && minItems != null) {
        val cardinality = itemCardinality(itemDataType)
        if (cardinality != null && minItems > cardinality)
          return failure("uniqueItems is true but minItems ($minItems) exceeds the item type cardinality ($cardinality)")
      }

      val dataType = ArrayDataType(name, itemDataType, isNullable, minItems, maxItems, uniqueItems)
      return if (enum.isEmpty())
        success(dataType)
      else
        AllowedValues
          .create(enum, dataType)
          .map { ArrayDataType(name, itemDataType, isNullable, minItems, maxItems, uniqueItems, it) }
    }

    private fun itemCardinality(itemDataType: DataType<out Any>): Long? = when {
      itemDataType.allowedValues != null                              -> itemDataType.allowedValues.size.toLong()
      itemDataType is BooleanDataType                                 -> 2L
      itemDataType is IntegerDataType && itemDataType.range.isBounded -> integerCardinality(itemDataType)
      itemDataType is NumberDataType && itemDataType.range.isBounded  -> numberCardinality(itemDataType)
      else                                                            -> null
    }

    private fun integerCardinality(dataType: IntegerDataType): Long {
      val min = effectiveIntMin(dataType.range)
      val max = effectiveIntMax(dataType.range)
      return if (dataType.multipleOf != null)
        multipleCount(min, max, dataType.multipleOf)
      else
        max.toLong() - min.toLong() + 1
    }

    private fun numberCardinality(dataType: NumberDataType): Long? {
      if (dataType.multipleOf == null) return null
      val range = dataType.range
      val first = range.minimum!!.divide(dataType.multipleOf, 0, CEILING).toLong()
      val last = range.maximum!!.divide(dataType.multipleOf, 0, FLOOR).toLong()
      val lower =
        if (range.exclusiveMinimum && isExactMultiple(first, dataType.multipleOf, range.minimum)) first + 1
        else first
      val upper =
        if (range.exclusiveMaximum && isExactMultiple(last, dataType.multipleOf, range.maximum)) last - 1
        else last

      return maxOf(0L, upper - lower + 1)
    }

    private fun isExactMultiple(n: Long, multipleOf: BigDecimal, bound: BigDecimal): Boolean =
      BigDecimal.valueOf(n).multiply(multipleOf).compareTo(bound) == 0

    private fun effectiveIntMin(range: Range): BigDecimal {
      val min = range.minimum!!.setScale(0, CEILING)
      return if (range.exclusiveMinimum) min + BigDecimal.ONE else min
    }

    private fun effectiveIntMax(range: Range): BigDecimal {
      val max = range.maximum!!.setScale(0, FLOOR)
      return if (range.exclusiveMaximum) max - BigDecimal.ONE else max
    }

    private fun multipleCount(min: BigDecimal, max: BigDecimal, multipleOf: BigDecimal): Long {
      val first = min.divide(multipleOf, 0, CEILING).toLong()
      val last = max.divide(multipleOf, 0, FLOOR).toLong()
      return maxOf(0L, last - first + 1)
    }
  }
}