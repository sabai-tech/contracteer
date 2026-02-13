package tech.sabai.contracteer.verifier

import tech.sabai.contracteer.core.datatype.*

object TypeMismatchMutation {

  fun mutate(dataType: DataType<out Any>): String? = when (dataType) {
    is StringDataType -> null
    is BinaryDataType -> null
    is AnyDataType    -> null
    else              -> "<<not a ${dataType.openApiType}>>"
  }
}
