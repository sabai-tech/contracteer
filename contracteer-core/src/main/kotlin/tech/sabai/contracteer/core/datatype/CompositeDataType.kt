package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.datatype.GenerationOutcome.*

/**
 * Base type for composite schemas (`allOf`, `anyOf`, `oneOf`) that validate a value
 * against the combination of their [subTypes].
 */
abstract class CompositeDataType<T: Any>(name: String,
                                         openApiType: String,
                                         internal val outerIsNullable: Boolean,
                                         val subTypes: List<DataType<out T>>,
                                         dataTypeClass: Class<out T>,
                                         allowedValues: AllowedValues? = null
): ResolvedDataType<T>(name, openApiType, dataTypeClass = dataTypeClass, allowedValues = allowedValues) {

  abstract val discriminator: Discriminator?

  /**
   * Computed lazily so unresolved [ProxyDataType] sub-types can resolve before the
   * per-kind rule is consulted. See [computeNullable] for the rule and cycle handling.
   */
  final override val isNullable: Boolean by lazy { computeNullable(CycleGuard()) }

  /**
   * Returns whether this composite admits null under the per-kind rule
   * (`anyOf: any`, `oneOf: count==1`, `allOf: all`), short-circuiting through
   * [outerIsNullable]. Threads [guard] across cyclic sub-branches; a re-entered
   * composite collapses to `false` for that branch, but [outerIsNullable] still
   * wins because it short-circuits the rule.
   *
   * Exposed as a separate method (rather than overriding the [isNullable] lazy
   * directly) so a walk into nested composites can thread the same [guard],
   * keeping cycle detection continuous across kinds.
   */
  protected abstract fun computeNullable(guard: CycleGuard<Boolean>): Boolean

  /**
   * Returns whether [dataType] admits null in the context of an enclosing composite walk.
   * Delegates to [computeNullable] for nested composites so [guard] threads across kinds.
   * An unresolved proxy collapses to `false` (no information is available).
   */
  protected fun isNullableBranch(dataType: DataType<*>, guard: CycleGuard<Boolean>): Boolean =
    when (dataType) {
      is ProxyDataType        -> guard.visit(dataType, onCycle = { false }) {
        if (dataType.isResolved) isNullableBranch(dataType.delegate, guard) else false
      }
      is CompositeDataType<*> -> dataType.computeNullable(guard)
      else                    -> dataType.isNullable
    }

  /**
   * Injects the discriminator property into [value] when both are present.
   *
   * [dataTypeName] identifies the schema whose mapping name to look up: the composite's
   * own name for `allOf` (the discriminated type itself), or the picked sub-type's name
   * for `oneOf`/`anyOf` (the sub-type identifies the variant).
   */
  @Suppress("UNCHECKED_CAST")
  protected fun injectDiscriminator(value: Any?, dataTypeName: String): Any? {
    val discriminator = this.discriminator
    return if (discriminator == null || value == null) value
    else (value as Map<String, Any?>) + (discriminator.propertyName to discriminator.getMappingName(dataTypeName))
  }

  /**
   * Walks the non-null sub-types in shuffled order and returns the first that produces a [Value],
   * with the discriminator injected. If every candidate produces [Boundary], the first such
   * boundary is propagated; if no candidate produces a [Value] (including the case where every
   * sub-type is [NullDataType]), falls back to [Reason.CYCLE].
   */
  protected fun generateFromAnySubType(ctx: GenerationContext): GenerationOutcome<Any> {
    var firstBoundary: Boundary? = null
    for (subType in subTypes.filterNot { it is NullDataType }.shuffled()) {
      when (val result = subType.randomValue(ctx).forProperty(subType.name)) {
        is Value    -> return Value(injectDiscriminator(result.value, subType.name))
        is Boundary -> firstBoundary = firstBoundary ?: result
      }
    }
    return firstBoundary ?: Boundary(Reason.CYCLE)
  }
}
