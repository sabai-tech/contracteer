package tech.sabai.contracteer.core.codec

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.serde.Serde

/**
 * Wraps a [DataType] that originated as a merged child of a composed schema.
 *
 * The wrapper prevents the dangerous pattern of reconstructing a synthetic `ObjectDataType`
 * from merged children and validating against it — such validation skips composed-schema
 * semantics. Codecs must always validate through the original (composed) DataType.
 *
 * Format-specific decoding lives at each serde (see `Serde.deserialize(raw, view)`),
 * not on this wrapper.
 *
 * Equality is identity-based on the wrapped source.
 */
internal class DecodeView private constructor(internal val source: DataType<out Any>) {

  fun shape(): Result<EncodingShape> = EncodingShape.of(source)

  override fun equals(other: Any?): Boolean = other is DecodeView && other.source === source
  override fun hashCode(): Int = System.identityHashCode(source)

  companion object {
    internal fun of(source: DataType<out Any>): DecodeView = DecodeView(source)
  }
}

internal fun Serde.deserialize(raw: String?, view: DecodeView): Result<Any?> =
  deserialize(raw, view.source)
