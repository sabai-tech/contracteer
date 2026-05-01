package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.datatype.GenerationOutcome.Boundary
import tech.sabai.contracteer.core.datatype.GenerationOutcome.Reason
import java.util.Collections
import java.util.IdentityHashMap

/**
 * Detects re-entry of the same [ProxyDataType] during one top-level random value synthesis.
 *
 * [ProxyDataType] uses [visit] to register itself before delegating; if the same proxy
 * instance is already on the stack, the call short-circuits with a [Boundary] of
 * [Reason.CYCLE]. Identity-based equality is intentional — two distinct proxy instances
 * that happen to share a name represent two distinct cycles.
 *
 * Single-threaded by design: one guard per top-level call, then discarded.
 */
class CycleGuard internal constructor() {

  // Identity-based set: cycle detection keys on the proxy instance, not on its name.
  private val visited: MutableSet<ProxyDataType> =
    Collections.newSetFromMap(IdentityHashMap())

  /**
   * Runs [body] while [proxy] is registered as visited. Returns [Boundary] of [Reason.CYCLE]
   * when [proxy] is already in the visited set (re-entry). Always removes [proxy] from the
   * set on exit, allowing siblings to use it without reporting a false cycle.
   */
  internal fun visit(proxy: ProxyDataType, body: () -> GenerationOutcome<Any>): GenerationOutcome<Any> {
    if (!visited.add(proxy)) return Boundary(Reason.CYCLE)
    try {
      return body()
    } finally {
      visited.remove(proxy)
    }
  }
}