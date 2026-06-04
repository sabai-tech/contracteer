package tech.sabai.contracteer.core.datatype

/**
 * State threaded through [DataType.randomValue] for one top-level synthesis.
 *
 * Composes the two concerns of a generation walk:
 * - [budget] bounds the recursion depth and total work performed by [ResolvedDataType].
 * - [cycleGuard] tracks visited proxies and is consulted by [ProxyDataType].
 *
 * Construct via [default] or [withBudget]; one instance per top-level call, then discarded.
 */
class GenerationContext private constructor(
  internal val budget: GenerationBudget,
  internal val cycleGuard: CycleGuard<GenerationOutcome<Any>>
) {

  companion object {
    private const val DEFAULT_MAX_DEPTH = 10
    private const val DEFAULT_MAX_NODES = 10_000

    @JvmStatic
    fun default(): GenerationContext = withBudget(DEFAULT_MAX_DEPTH, DEFAULT_MAX_NODES)

    @JvmStatic
    fun withBudget(maxDepth: Int, maxNodes: Int): GenerationContext =
      GenerationContext(GenerationBudget(maxDepth, maxNodes), CycleGuard())
  }
}