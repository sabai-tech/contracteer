package tech.sabai.contracteer.mockserver

import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.datatype.IntegerDataType
import tech.sabai.contracteer.core.datatype.ObjectDataType
import tech.sabai.contracteer.core.datatype.StringDataType

object DataTypeFixture {

  fun integerDataType(isNullable: Boolean = false,
                      enum: List<Any?> = emptyList()) =
    IntegerDataType.create("integer", isNullable, enum).value!!

  fun objectDataType(properties: Map<String, DataType<*>>,
                     requiredProperties: Set<String> = emptySet(),
                     isNullable: Boolean = false,
                     enum: List<Any?> = emptyList()) =
    ObjectDataType.create("object", properties, requiredProperties, isNullable, enum).value!!

  fun stringDataType(isNullable: Boolean = false,
                     enum: List<Any?> = emptyList()) =
    StringDataType.create("string", "string", isNullable, enum).value!!
}