package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.datatype.GenerationOutcome.Boundary
import tech.sabai.contracteer.core.datatype.GenerationOutcome.Reason

/**
 * Bounds the work performed during one top-level random value synthesis.
 *
 * Tracks recursion [depth] (capped by [maxDepth]) and a remaining [nodes] counter that is
 * decremented for each unit of work. When either cap is reached, [step] short-circuits with
 * a [Boundary] carrying the appropriate [Reason]. Used by [ResolvedDataType] around every
 * call to its `doRandomValue` to guarantee termination on recursive or oversized schemas.
 *
 * Single-threaded by design: one budget per top-level call, then discarded.
 */
class GenerationBudget internal constructor(
  private val maxDepth: Int,
  initialNodes: Int
) {

  private var depth: Int = 0
  private var nodes: Int = initialNodes

  /**
   * Runs [body] within the budget, charging one node and one depth level for the call.
   * Returns [Boundary] of [Reason.DEPTH] or [Reason.NODES] when the corresponding cap is
   * reached, without invoking [body].
   */
  internal fun <T> step(body: () -> GenerationOutcome<T>): GenerationOutcome<T> = when {
    depth >= maxDepth -> Boundary(Reason.DEPTH)
    nodes == 0        -> Boundary(Reason.NODES)
    else              -> charged(body)
  }

  /** Caps [count] by the remaining node budget so callers do not request more than can be produced. */
  internal fun limit(count: Int): Int = minOf(count, nodes)

  private fun <R> charged(body: () -> R): R {
    depth++
    nodes--
    try {
      return body()
    } finally {
      depth--
    }
  }
}