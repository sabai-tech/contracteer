package tech.sabai.contracteer.core.serde

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.normalize

/**
 * Serialization and deserialization strategy for content exchanged over HTTP.
 *
 * Values are [normalized][normalize] before serialization and after deserialization
 * so that values are structurally equal regardless of their origin.
 */
sealed class Serde {
  /** Normalizes the [value] and serializes it into its string representation for transmission over HTTP. */
  fun serialize(value: Any?): String =
    doSerialize(value?.normalize())

  /** Deserializes a string received over HTTP into a typed value matching the [targetDataType], then normalizes it. */
  fun deserialize(source: String?, targetDataType: DataType<out Any>): Result<Any?> =
    doDeserialize(source, targetDataType).map { it?.normalize() }

  /** Implementation-specific serialization. Called by [serialize], which normalizes the input. */
  protected abstract fun doSerialize(value: Any?): String

  /** Implementation-specific deserialization. Called by [deserialize], which normalizes the result. */
  protected abstract fun doDeserialize(source: String?, targetDataType: DataType<out Any>): Result<Any?>
}
