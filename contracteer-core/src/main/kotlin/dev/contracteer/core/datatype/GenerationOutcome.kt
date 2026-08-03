package dev.contracteer.core.datatype

import dev.contracteer.core.Result

/**
 * Outcome of [DataType.randomValue] under a [GenerationContext].
 *
 * `Value` carries a generated value (possibly null for nullable schemas or for an enum
 * whose only permitted value is null). `Boundary` reports that the schema could not be
 * synthesized at the current path because of a cycle, depth cap, or node-budget cap;
 * containers decide whether to absorb, omit, or propagate it based on slot semantics.
 */
sealed interface GenerationOutcome<out T> {

  data class Value<T>(val value: T?): GenerationOutcome<T>

  data class Boundary(val reason: Reason, val path: String = ""): GenerationOutcome<Nothing>

  /** Returns a new result with [propertyName] prepended to a [Boundary]'s path; identity on [Value]. */
  fun forProperty(propertyName: String): GenerationOutcome<T> =
    when (this) {
      is Value    -> this
      is Boundary -> copy(path = buildPath(propertyName, path))
    }

  /** Returns a new result with the array index `[index]` prepended to a [Boundary]'s path; identity on [Value]. */
  fun forIndex(index: Int): GenerationOutcome<T> = forProperty("[$index]")

  /**
   * Converts this outcome to a [Result]. [Value] becomes [Result.success]; [Boundary] becomes
   * [Result.failure] with the boundary's path as the property path and an end-user-facing
   * explanation of the reason as the error message.
   */
  fun toResult(): Result<T?> = when (this) {
    is Value    -> Result.success(value)
    is Boundary -> Result.failure(path, reason.explanation())
  }

  enum class Reason {
    CYCLE,
    DEPTH,
    NODES,
    NAMES;

    /** Human-readable guidance describing why the boundary fired and how to relax the schema. */
    fun explanation(): String = when (this) {
      CYCLE -> "schema is recursive; the path shows the cycling properties — break the cycle by " +
               "making one nullable or optional, or by removing minItems from an array on this path"
      DEPTH -> "schema is too deeply nested for generation; flatten the structure or relax required nested properties"
      NODES -> "schema requires more values than the generation budget allows; " +
               "reduce minItems or minProperties along this path"
      NAMES -> "could not synthesize enough property names that satisfy the propertyNames schema; " +
               "relax the pattern, length, or enum on propertyNames, or reduce minProperties on this path"
    }
  }

  companion object {
    private fun buildPath(segment: String, existing: String): String =
      when {
        segment.isEmpty()        -> existing
        existing.isEmpty()       -> segment
        existing.startsWith("[") -> "$segment$existing"
        else                     -> "$segment.$existing"
      }
  }
}