package tech.sabai.contracteer.core

import tech.sabai.contracteer.core.Result.Failure
import tech.sabai.contracteer.core.Result.Success
import tech.sabai.contracteer.core.dsl.integerType
import tech.sabai.contracteer.core.dsl.objectType

// Test assertion helpers
fun <T> Result<T>.assertSuccess(): T = when (this) {
  is Success -> value
  is Failure -> throw AssertionError("Expected success but got errors: ${errors()}")
}

fun <T> Result<T>.assertFailure(): List<String> = when (this) {
  is Failure -> errors()
  is Success -> throw AssertionError("Expected failure but got success with value: $value")
}

fun <T> List<T>.assertSingle(): T {
  assert(size == 1) { "Expected single element but got $size" }
  return single()
}

fun rgbObjectType() = objectType {
  properties {
    "R" to integerType()
    "G" to integerType()
    "B" to integerType()
  }
}
