package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result

/**
 * A proxy for a [DataType] that is currently being converted.
 *
 * Used to break circular schema references during conversion. The [delegate] is set
 * after the real DataType conversion completes, and all operations are forwarded to it.
 *
 * The class also handles the cycle-detection half of random value generation:
 * [randomValue] registers the proxy with the [GenerationContext] before delegating, and
 * returns a [GenerationOutcome.Boundary] of [GenerationOutcome.Reason.CYCLE] when the same
 * proxy is re-entered. Budget accounting (depth, nodes) lives on [ResolvedDataType] and
 * applies to the delegate's call.
 */
class ProxyDataType internal constructor(override val name: String): DataType<Any> {

  internal lateinit var delegate: DataType<out Any>

  internal val isResolved: Boolean get() = ::delegate.isInitialized

  override val openApiType: String get() = delegate.openApiType
  override val isNullable: Boolean get() = delegate.isNullable
  override val dataTypeClass: Class<out Any> get() = delegate.dataTypeClass
  override val allowedValues: AllowedValues? get() = delegate.allowedValues
  override fun validate(value: Any?): Result<Any?> = delegate.validate(value)

  override fun randomValue(ctx: GenerationContext): GenerationOutcome<Any> =
    ctx.cycleGuard.visit(this) { delegate.randomValue(ctx) }

  /**
   * Returns `true` when the delegate is fully structured, or when the proxy is still being
   * resolved during conversion. Reporting `true` for an unresolved proxy lets composite
   * validation (e.g. `allOf` subtypes checks) accept the placeholder; once resolution completes,
   * the answer comes from the real delegate.
   */
  override fun isFullyStructured(): Boolean = !isResolved || delegate.isFullyStructured()

  override fun asRequestType(): DataType<Any> = this

  override fun asResponseType(): DataType<Any> = this
}