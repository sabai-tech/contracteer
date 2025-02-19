package tech.sabai.contracteer.verifier

import tech.sabai.contracteer.core.datatype.*

object DataTypeFixture {
  fun arrayDataType(itemDataType: DataType<out Any>,
                    isNullable: Boolean = false,
                    enum: List<Any?> = emptyList()) =
    ArrayDataType.create("array", itemDataType, isNullable, enum).value!!

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