package dev.contracteer.core.datatype

import dev.contracteer.core.datatype.GenerationOutcome.Boundary
import dev.contracteer.core.datatype.GenerationOutcome.Value

/**
 * Generates a random value under a fresh [GenerationContext]. Throws on [Boundary] so any
 * test that silently relied on a legacy cycle fallback fails loud rather than continuing on
 * a hidden behavior change. Tests that intentionally probe boundary outcomes should call
 * [DataType.randomValue] with an explicit [GenerationContext] instead.
 */
fun <T : Any> DataType<T>.randomValue(): T? =
  when (val result = randomValue(GenerationContext.default())) {
    is Value    -> result.value
    is Boundary -> error("randomValue() boundaried in test: ${result.reason} at '${result.path}'")
  }