package tech.sabai.contracteer.core

import tech.sabai.contracteer.core.datatype.*
import java.math.BigDecimal

object DataTypeFixture {

  fun allOfDataType(
    subTypes: List<StructuredObjectDataType>,
    isNullable: Boolean = false,
    enum: List<Any?> = emptyList()) =
    AllOfDataType.create("allOf", subTypes, isNullable, enum).value!!

  fun anyOfDataType(subTypes: List<StructuredObjectDataType>,
                    discriminator: Discriminator? = null,
                    isNullable: Boolean = false,
                    enum: List<Any?> = emptyList()) =
    AnyOfDataType.create("anyOf", subTypes, discriminator, isNullable, enum).value!!

  fun arrayDataType(itemDataType: DataType<out Any>,
                    isNullable: Boolean = false,
                    enum: List<Any?> = emptyList()) =
    ArrayDataType.create("array", itemDataType, isNullable, enum).value!!

  fun base64DataType(isNullable: Boolean = false,
                     enum: List<Any?> = emptyList()) =
    Base64DataType.create("base64", isNullable, enum).value!!

  fun binaryDataType(isNullable: Boolean = false,
                     enum: List<Any?> = emptyList()) =
    BinaryDataType.create("binary", isNullable, enum).value!!

  fun booleanDataType(isNullable: Boolean = false,
                      enum: List<Any?> = emptyList()) =
    BooleanDataType.create("boolean", isNullable, enum).value!!

  fun dateDataType(isNullable: Boolean = false,
                   enum: List<Any?> = emptyList()) =
    DateDataType.create("date", isNullable, enum).value!!

  fun dateTimeDataType(isNullable: Boolean = false,
                       enum: List<Any?> = emptyList()) =
    DateTimeDataType.create("dateTime", isNullable, enum).value!!

  fun emailDataType(isNullable: Boolean = false,
                    enum: List<Any?> = emptyList()) =
    EmailDataType.create("email", isNullable, enum).value!!

  fun integerDataType(isNullable: Boolean = false,
                      enum: List<Any?> = emptyList(),
                      minimum: BigDecimal? = null,
                      maximum: BigDecimal? = null,
                      exclusiveMinimum: Boolean = false,
                      exclusiveMaximum: Boolean = false) =
    IntegerDataType.create("integer", isNullable, enum, minimum, maximum, exclusiveMinimum, exclusiveMaximum).value!!

  fun numberDataType(isNullable: Boolean = false,
                     enum: List<Any?> = emptyList(),
                     minimum: BigDecimal? = null,
                     maximum: BigDecimal? = null,
                     exclusiveMinimum: Boolean = false,
                     exclusiveMaximum: Boolean = false) =
    NumberDataType.create("number", isNullable, enum, minimum, maximum, exclusiveMinimum, exclusiveMaximum).value!!

  fun objectDataType(name: String = "object",
                     properties: Map<String, DataType<*>>,
                     requiredProperties: Set<String> = emptySet(),
                     isNullable: Boolean = false,
                     enum: List<Any?> = emptyList()) =
    ObjectDataType.create(name, properties, requiredProperties, isNullable, enum).value!!

  fun oneOfDataType(subTypes: List<StructuredObjectDataType>,
                    discriminator: Discriminator? = null,
                    isNullable: Boolean = false,
                    enum: List<Any?> = emptyList()) =
    OneOfDataType.create("oneOf", subTypes, discriminator, isNullable, enum).value!!

  fun stringDataType(isNullable: Boolean = false,
                     enum: List<Any?> = emptyList()) =
    StringDataType.create("string", "string", isNullable, enum).value!!

  fun uuidDataType(isNullable: Boolean = false,
                   enum: List<Any?> = emptyList()) =
    UuidDataType.create("uuid", isNullable, enum).value!!
}