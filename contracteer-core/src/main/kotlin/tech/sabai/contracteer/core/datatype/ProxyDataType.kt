package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result

/**
 * A proxy for a [DataType] that is currently being converted.
 *
 * Used to break circular schema references during conversion. The [delegate] is set
 * after the real DataType conversion completes, and all operations are forwarded to it.
 */
class ProxyDataType internal constructor(override val name: String): DataType<Any> {

  internal lateinit var delegate: DataType<out Any>

  override val openApiType: String get() = delegate.openApiType
  override val isNullable: Boolean get() = delegate.isNullable
  override val dataTypeClass: Class<out Any> get() = delegate.dataTypeClass
  override val allowedValues: AllowedValues? get() = delegate.allowedValues

  override fun validate(value: Any?): Result<Any?> = delegate.validate(value)

  override fun randomValue(): Any? {
    val visited = generatingProxies.get()
    if (!visited.add(this)) return null
    return try { delegate.randomValue() } finally { visited.remove(this) }
  }

  override fun isFullyStructured(): Boolean = delegate.isFullyStructured()

  override fun asRequestType(): DataType<Any> = this

  override fun asResponseType(): DataType<Any> = this

  companion object {
    private val generatingProxies = ThreadLocal.withInitial { mutableSetOf<ProxyDataType>() }
  }
}