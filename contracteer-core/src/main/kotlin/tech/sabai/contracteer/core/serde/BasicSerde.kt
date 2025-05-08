package tech.sabai.contracteer.core.serde

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.datatype.AnyDataType
import tech.sabai.contracteer.core.datatype.ArrayDataType
import tech.sabai.contracteer.core.datatype.Base64DataType
import tech.sabai.contracteer.core.datatype.BinaryDataType
import tech.sabai.contracteer.core.datatype.BooleanDataType
import tech.sabai.contracteer.core.datatype.CompositeDataType
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.datatype.DateDataType
import tech.sabai.contracteer.core.datatype.DateTimeDataType
import tech.sabai.contracteer.core.datatype.EmailDataType
import tech.sabai.contracteer.core.datatype.IntegerDataType
import tech.sabai.contracteer.core.datatype.NumberDataType
import tech.sabai.contracteer.core.datatype.ObjectDataType
import tech.sabai.contracteer.core.datatype.StringDataType
import tech.sabai.contracteer.core.datatype.UuidDataType

object BasicSerde: Serde {
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