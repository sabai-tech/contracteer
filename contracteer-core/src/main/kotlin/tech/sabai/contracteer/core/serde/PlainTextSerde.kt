package tech.sabai.contracteer.core.serde

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.datatype.*

/** [Serde] implementation for `text/plain` content. Supports scalar types only; objects and arrays are rejected. */
object PlainTextSerde: Serde {
  override fun serialize(value: Any?) =
    value.toString()

  override fun deserialize(source: String?, targetDataType: DataType<out Any>): Result<Any?> =
    if (source == null) success(null)
    else
      when (targetDataType) {
        is CompositeDataType, is ObjectDataType -> failure(targetDataType.name, "'object' is not supported")
        is ArrayDataType                        -> failure(targetDataType.name, "'array' are not supported")
        is BooleanDataType                      -> source.asBoolean()
        is NumberDataType, is IntegerDataType   -> source.asBigDecimal()
        is AnyDataType, is StringDataType,
        is UuidDataType, is Base64DataType,
        is BinaryDataType, is EmailDataType,
        is DateTimeDataType, is DateDataType    -> success(source)
      }

  private fun String.asBoolean() =
    toBooleanStrictOrNull()?.let { success(it) } ?: failure("Wrong type. Expected type: 'boolean'")

  private fun String.asBigDecimal() =
    toBigDecimalOrNull()?.let { success(it) } ?: failure("Wrong type. Expected type: 'number' or 'integer'")
}