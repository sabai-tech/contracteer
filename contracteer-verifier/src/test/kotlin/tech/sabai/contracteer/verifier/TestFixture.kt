package tech.sabai.contracteer.verifier

import tech.sabai.contracteer.core.contract.Body
import tech.sabai.contracteer.core.contract.ContentType
import tech.sabai.contracteer.core.contract.ContractParameter
import tech.sabai.contracteer.core.contract.Example
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
                     additionalProperties: Boolean = true,
                     isNullable: Boolean = false,
                     enum: List<Any?> = emptyList()) =
    ObjectDataType.create("object", properties, requiredProperties, additionalProperties, isNullable, enum).value!!

  fun stringDataType(isNullable: Boolean = false,
                     enum: List<String?> = emptyList(),
                     minLength: Int? = null,
                     maxLength: Int? = null) =
    StringDataType.create("string", "string", isNullable, enum, minLength, maxLength).value!!

  fun body(contentType: ContentType, dataType: DataType<out Any>, example: Example? = null) =
    Body.create(contentType, dataType, example).value!!

  fun pathParameter(name: String, dataType: DataType<out Any>, example: Example? = null) =
    ContractParameter.create(name, dataType, true, example).value!!

  fun parameter(name: String, dataType: DataType<out Any>, isRequired: Boolean = false, example: Example? = null) =
    ContractParameter.create(name, dataType, isRequired, example).value!!
}