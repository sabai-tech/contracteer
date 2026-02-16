package tech.sabai.contracteer.verifier

import tech.sabai.contracteer.core.datatype.AnyDataType
import tech.sabai.contracteer.core.datatype.BinaryDataType
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.datatype.StringDataType

internal object TypeMismatchMutation {

  fun mutate(dataType: DataType<out Any>): String? = when (dataType) {
    is StringDataType -> null
    is BinaryDataType -> null
    is AnyDataType    -> null
    else              -> "<<not a ${dataType.openApiType}>>"
  }
}
