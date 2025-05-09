package tech.sabai.contracteer.mockserver

import tech.sabai.contracteer.core.contract.Body
import tech.sabai.contracteer.core.contract.ContentType
import tech.sabai.contracteer.core.contract.ContractParameter
import tech.sabai.contracteer.core.contract.Example
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.datatype.IntegerDataType
import tech.sabai.contracteer.core.datatype.ObjectDataType
import tech.sabai.contracteer.core.datatype.StringDataType
import java.math.BigDecimal

object TestFixture {

  fun integerDataType(isNullable: Boolean = false,
                      enum: List<BigDecimal?> = emptyList()) =
    IntegerDataType.create("integer", isNullable, enum).value!!

  fun objectDataType(properties: Map<String, DataType<out Any>>,
                     requiredProperties: Set<String> = emptySet(),
                     allowAdditionalProperties: Boolean = true,
                     additionalPropertiesDataType: DataType<out Any>? = null,
                     isNullable: Boolean = false,
                     enum: List<Any?> = emptyList()) =
    ObjectDataType.create("object", properties, requiredProperties, allowAdditionalProperties, additionalPropertiesDataType, isNullable, enum).value!!

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