package tech.sabai.contracteer.core.datatype

import java.util.Collections
import java.util.IdentityHashMap

/**
 * Generic single-thread cycle detector keyed on [ProxyDataType] identity.
 *
 * Each top-level walk creates one guard then discards it. Re-entering the same
 * proxy instance during the walk short-circuits to the caller-supplied [onCycle]
 * value; otherwise [body] runs with the proxy registered as visited, and the
 * registration is removed on exit (success or failure). Identity-based equality
 * is intentional — two distinct proxy instances that happen to share a name
 * represent two distinct cycles.
 *
 * Three sites share this primitive:
 * - [ProxyDataType.randomValue] (onCycle returns a [GenerationOutcome.Boundary])
 * - [tech.sabai.contracteer.core.codec.EncodingShape] (onCycle returns the cycle sentinel)
 * - composite lazy [DataType.isNullable] computation (onCycle returns `false`)
 */
class CycleGuard<R> internal constructor() {

  private val visited: MutableSet<ProxyDataType> =
    Collections.newSetFromMap(IdentityHashMap())

  /**
   * Runs [body] with [proxy] registered as visited and returns its result.
   * If [proxy] is already on the visited stack (cyclic re-entry), short-circuits
   * by returning [onCycle]'s value. Always removes [proxy] on exit, allowing
   * siblings to use it without reporting a false cycle.
   */
  internal fun visit(proxy: ProxyDataType, onCycle: () -> R, body: () -> R): R =
    if (!visited.add(proxy)) onCycle()
    else try { body() } finally { visited.remove(proxy) }
}
