package dev.blitzcraft.contracts.core.contract

import dev.blitzcraft.contracts.core.normalize
import dev.blitzcraft.contracts.core.Result
import dev.blitzcraft.contracts.core.Result.Companion.failure
import dev.blitzcraft.contracts.core.Result.Companion.success
import dev.blitzcraft.contracts.core.accumulate

private const val VALUE_DOES_NOT_MATCH = "value does not match. Expected: %s, Actual: %s"

class Example(value: Any?) {
  val normalizedValue = value?.normalize()

  fun matches(other: Any?) = normalizedValue.matchesValue(other?.normalize())

  private fun Any?.matchesValue(other: Any?): Result<Any?> =
    when {
      this == other                           -> success(this)
      this is Map<*, *> && other is Map<*, *> -> matches(other)
      this is Array<*> && other is Array<*>   -> matches(other)
      else                                    -> failure(VALUE_DOES_NOT_MATCH.format(this, other))
    }

  private fun Map<*, *>.matches(other: Map<*, *>): Result<Map<*, *>> =
    if (this.keys != other.keys) failure("Property names are not equal")
    else accumulate { it.value.matchesValue(other[it.key]).forProperty(it.key.toString()) }.map { other }

  private fun Array<*>.matches(other: Array<*>): Result<Array<*>> =
    if (size != other.size) failure("Array size does not match")
    else accumulate { index, item -> item.matchesValue(other[index]).forIndex(index) }.map { other }
}

