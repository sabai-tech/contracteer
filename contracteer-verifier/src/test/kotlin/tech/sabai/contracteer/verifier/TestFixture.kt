package tech.sabai.contracteer.verifier

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Failure
import tech.sabai.contracteer.core.Result.Success
import tech.sabai.contracteer.core.datatype.*
import java.math.BigDecimal

fun <T> Result<T>.assertSuccess(): T = when (this) {
  is Success -> value
  is Failure -> throw AssertionError("Expected success but got errors: ${errors()}")
}

object TestFixture {
  fun arrayDataType(itemDataType: DataType<out Any>,
                    isNullable: Boolean = false,
                    enum: List<Any?> = emptyList()) =
    ArrayDataType.create("array", itemDataType, isNullable, enum).assertSuccess()

  fun integerDataType(isNullable: Boolean = false,
                      enum: List<BigDecimal?> = emptyList()) =
    IntegerDataType.create("integer", isNullable, enum).assertSuccess()

  fun objectDataType(properties: Map<String, DataType<out Any>>,
                     requiredProperties: Set<String> = emptySet(),
                     allowAdditionalProperties: Boolean = true,
                     additionalPropertiesDataType: DataType<out Any>? = null,
                     isNullable: Boolean = false,
                     enum: List<Any?> = emptyList()) =
    ObjectDataType.create(name = "object", properties = properties, requiredProperties = requiredProperties, allowAdditionalProperties = allowAdditionalProperties, additionalPropertiesDataType = additionalPropertiesDataType, isNullable = isNullable, enum = enum).assertSuccess()

  fun stringDataType(isNullable: Boolean = false,
                     enum: List<String?> = emptyList(),
                     minLength: Int? = null,
                     maxLength: Int? = null) =
    StringDataType.create("string", "string", isNullable, enum, minLength, maxLength).assertSuccess()
}
