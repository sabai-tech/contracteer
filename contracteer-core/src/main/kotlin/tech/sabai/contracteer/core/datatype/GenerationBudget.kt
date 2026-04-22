package tech.sabai.contracteer.core.datatype

/**
 * Caps a single random value generation by a maximum recursion depth and a
 * maximum total number of values produced.
 *
 * A budget is an accounting object: it tracks how many levels of recursion
 * and how many values are still available, and reports exhaustion through
 * [tryConsume]. It does not decide what to do when exhausted — callers do.
 */
internal class GenerationBudget private constructor(
  private val maxDepth: Int,
  initialNodes: Int
) {

  private var depth: Int = 0
  private var nodes: Int = initialNodes

  /** Accounts for one more generated value. Returns `false` when depth or node limits are reached. */
  fun tryConsume(): Boolean {
    if (depth >= maxDepth || nodes == 0) return false
    depth++
    nodes--
    return true
  }

  /** Releases one level of recursion after a successful [tryConsume]. */
  fun release() {
    depth--
  }

  companion object {
    const val DEFAULT_MAX_DEPTH = 10
    const val DEFAULT_MAX_NODES = 10_000

    private val threadLocal = ThreadLocal<GenerationBudget?>()

    /** Returns [requested] capped by the active budget's remaining capacity, or [requested] unchanged when no budget is active. */
    fun limit(requested: Int): Int =
      threadLocal.get()?.let { minOf(requested, it.nodes) } ?: requested

    /**
     * Runs [block] under an active budget, consuming one recursion level.
     * Installs a fresh budget for the outermost call, reuses the existing one for nested calls.
     * Returns `null` when the budget is exhausted before [block] could run.
     */
    inline fun <T> consume(block: () -> T): T? {
      val existing = threadLocal.get()
      val budget = existing ?: GenerationBudget(DEFAULT_MAX_DEPTH, DEFAULT_MAX_NODES).also { threadLocal.set(it) }
      try {
        if (!budget.tryConsume()) return null
        return try {
          block()
        } finally {
          budget.release()
        }
      } finally {
        if (existing == null) threadLocal.remove()
      }
    }
  }
}