package tech.sabai.contracteer.core.contract

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.accumulate
import tech.sabai.contracteer.core.accumulateWithIndex
import tech.sabai.contracteer.core.normalize

private const val VALUE_DOES_NOT_MATCH = "value does not match. Expected: %s, Actual: %s"

class Example(value: Any?) {
  val normalizedValue = value?.normalize()

  fun matches(other: Any?) = normalizedValue.matchesValue(other?.normalize())

  private fun Any?.matchesValue(other: Any?): Result<Any?> =
    when {
      this == other                           -> success(this)
      this is Map<*, *> && other is Map<*, *> -> matches(other)
      this is List<*> && other is List<*>     -> matches(other)
      else                                    -> failure(VALUE_DOES_NOT_MATCH.format(this, other))
    }

  private fun Map<*, *>.matches(other: Map<*, *>): Result<Map<*, *>> =
    if (this.keys != other.keys) failure("Property names are not equal")
    else accumulate { it.value.matchesValue(other[it.key]).forProperty(it.key.toString()) }.map { other }

  private fun List<*>.matches(other: List<*>): Result<List<*>> =
    if (size != other.size) failure("Array size does not match")
    else accumulateWithIndex { index, item -> item.matchesValue(other[index]).forIndex(index) }.map { other }
}

