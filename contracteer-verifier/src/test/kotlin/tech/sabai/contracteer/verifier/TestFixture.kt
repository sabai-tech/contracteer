package tech.sabai.contracteer.verifier

import tech.sabai.contracteer.core.datatype.*
import java.math.BigDecimal

object TestFixture {
  fun arrayDataType(itemDataType: DataType<out Any>,
                    isNullable: Boolean = false,
                    enum: List<Any?> = emptyList()) =
    ArrayDataType.create("array", itemDataType, isNullable, enum).value!!

  fun integerDataType(isNullable: Boolean = false,
                      enum: List<BigDecimal?> = emptyList()) =
    IntegerDataType.create("integer", isNullable, enum).value!!

  fun objectDataType(properties: Map<String, DataType<out Any>>,
                     requiredProperties: Set<String> = emptySet(),
                     allowAdditionalProperties: Boolean = true,
                     additionalPropertiesDataType: DataType<out Any>? = null,
                     isNullable: Boolean = false,
                     enum: List<Any?> = emptyList()) =
    ObjectDataType.create(name = "object", properties = properties, requiredProperties = requiredProperties, allowAdditionalProperties = allowAdditionalProperties, additionalPropertiesDataType = additionalPropertiesDataType, isNullable = isNullable, enum = enum).value!!

  fun stringDataType(isNullable: Boolean = false,
                     enum: List<String?> = emptyList(),
                     minLength: Int? = null,
                     maxLength: Int? = null) =
    StringDataType.create("string", "string", isNullable, enum, minLength, maxLength).value!!
}
