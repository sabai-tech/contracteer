package dev.blitzcraft.contracts.core.contract

import dev.blitzcraft.contracts.core.normalize
import dev.blitzcraft.contracts.core.validation.ValidationResult
import dev.blitzcraft.contracts.core.validation.ValidationResult.Companion.success
import dev.blitzcraft.contracts.core.validation.ValidationResult.Companion.error
import dev.blitzcraft.contracts.core.validation.validateEach

private const val VALUE_DOES_NOT_MATCH = "value does not match. Expected: %s, Actual: %s"

class Example(value: Any?) {
  val normalizedValue = value?.normalize()

  fun matches(other: Any?) = normalizedValue.matchesValue(other?.normalize())

  private fun Any?.matchesValue(other: Any?) =
    when {
      this == other                           -> success()
      this is Map<*, *> && other is Map<*, *> -> matches(other)
      this is Array<*> && other is Array<*>   -> matches(other)
      else                                    -> error(VALUE_DOES_NOT_MATCH.format(this, other))
    }

  private fun Map<*, *>.matches(other: Map<*, *>): ValidationResult =
    if (keys != other.keys) error("Property names are not equal")
    else validateEach { it.value.matchesValue(other[it.key]).forProperty(it.key.toString()) }


  private fun Array<*>.matches(other: Array<*>): ValidationResult =
    if (size != other.size) error("Array size does not match")
    else validateEach { index, item -> item.matchesValue(other[index]).forIndex(index) }
}

