package tech.sabai.contracteer.core.serde

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.datatype.DataType

sealed interface Serde {
  fun serialize(value: Any?): String
  fun deserialize(source: String?, targetDataType: DataType<out Any>): Result<Any?>
}
