package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.accumulateWithIndex

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
    val max = maxItems ?: maxOf(min, 5)
    return List((min..max).random()) { itemDataType.randomValue() }
  }

  companion object {
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

      val dataType = ArrayDataType(name, itemDataType, isNullable, minItems, maxItems, uniqueItems)
      return if (enum.isEmpty())
        success(dataType)
      else
        AllowedValues
          .create(enum, dataType)
          .map { ArrayDataType(name, itemDataType, isNullable, minItems, maxItems, uniqueItems, it) }
    }
  }
}