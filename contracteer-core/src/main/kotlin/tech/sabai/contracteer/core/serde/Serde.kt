package tech.sabai.contracteer.core.serde

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.datatype.DataType

/** Serialization and deserialization strategy for content exchanged over HTTP. */
sealed interface Serde {
  /** Serializes a value into its string representation for transmission over HTTP. */
  fun serialize(value: Any?): String

  /** Deserializes a string received over HTTP into a typed value matching the [targetDataType]. */
  fun deserialize(source: String?, targetDataType: DataType<out Any>): Result<Any?>
}
