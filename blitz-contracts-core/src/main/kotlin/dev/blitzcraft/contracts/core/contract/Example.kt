package dev.blitzcraft.contracts.core.contract

import dev.blitzcraft.contracts.core.convert
import dev.blitzcraft.contracts.core.validation.ValidationResult
import dev.blitzcraft.contracts.core.validation.ValidationResult.Companion.success
import dev.blitzcraft.contracts.core.validation.ValidationResult.Companion.error
import dev.blitzcraft.contracts.core.validation.validate

private const val VALUE_DOES_NOT_MATCH = "value does not match. Expected: %s, Actual: %s"

class Example(aValue: Any?) {
  val value = aValue.convert()

  fun validate(other: Any?) =
    when {
      other == value                           -> success()
      other is Array<*> && value is Array<*>   -> validateArray(value, other)
      other is Map<*, *> && value is Map<*, *> -> validateMap(value, other)
      else                                     -> error(VALUE_DOES_NOT_MATCH.format(value, other))
    }

  private fun validateMap(value: Map<*, *>, other: Map<*, *>): ValidationResult =
    if (value.keys != other.keys) error("Property names are not equal")
    else value.validate {
      when {
        it.value is Map<*, *> && other[it.key] is Map<*, *> -> validateMap(it.value as Map<*, *>, other[it.key] as Map<*, *>).forProperty(it.key.toString())
        it.value is Array<*> && other[it.key] is Array<*>   -> validateArray(it.value as Array<*>, other[it.key] as Array<*>).forProperty(it.key.toString())
        it.value == other[it.key]                           -> success()
        else                                                -> error(VALUE_DOES_NOT_MATCH.format(it.value, other[it.key])).forProperty(it.key.toString())
      }
    }

  private fun validateArray(value: Array<*>, other: Array<*>): ValidationResult =
    if (other.size != value.size) error("array size does not match")
    else value.validate { index, item ->
      when {
        item is Array<*> && other[index] is Array<*>   -> validateArray(item, other[index] as Array<*>).forIndex(index)
        item is Map<*, *> && other[index] is Map<*, *> -> validateMap(item, other[index] as Map<*, *>).forIndex(index)
        item == other[index]                           -> success()
        else                                           -> error(index, VALUE_DOES_NOT_MATCH.format(item, other[index]))
      }
    }
}
