package tech.sabai.contracteer.core

import tech.sabai.contracteer.core.datatype.*
import java.math.BigDecimal

object DataTypeFixture {

  fun allOfDataType(
    name: String = "allOf",
    subTypes: List<ObjectDataType>,
    isNullable: Boolean = false,
    discriminator: Discriminator? = null,
    enum: List<Any?> = emptyList()) =
    AllOfDataType.create(name, subTypes, isNullable, discriminator, enum).value!!

  fun anyOfDataType(name: String = "anyOf",
                    subTypes: List<DataType<out Any>>,
                    discriminator: Discriminator? = null,
                    isNullable: Boolean = false,
                    enum: List<Any?> = emptyList()) =
    AnyOfDataType.create(name, subTypes, discriminator, isNullable, enum).value!!

  fun arrayDataType(itemDataType: DataType<out Any>,
                    isNullable: Boolean = false,
                    enum: List<Any?> = emptyList()) =
    ArrayDataType.create("array", itemDataType, isNullable, enum).value!!

  fun base64DataType(isNullable: Boolean = false,
                     enum: List<String?> = emptyList(),
                     minLength: Int? = null,
                     maxLength: Int? = null) =
    Base64DataType.create("base64", isNullable, enum, minLength, maxLength).value!!

  fun binaryDataType(isNullable: Boolean = false,
                     enum: List<String?> = emptyList(),
                     minLength: Int? = null,
                     maxLength: Int? = null) =
    BinaryDataType.create("binary", isNullable, enum, minLength, maxLength).value!!

  fun booleanDataType(isNullable: Boolean = false,
                      enum: List<Boolean?> = emptyList()) =
    BooleanDataType.create("boolean", isNullable, enum).value!!

  fun dateDataType(isNullable: Boolean = false,
                   enum: List<String?> = emptyList()) =
    DateDataType.create("date", isNullable, enum).value!!

  fun dateTimeDataType(isNullable: Boolean = false,
                       enum: List<String?> = emptyList()) =
    DateTimeDataType.create("dateTime", isNullable, enum).value!!

  fun emailDataType(isNullable: Boolean = false,
                    enum: List<String?> = emptyList(),
                    minLength: Int? = null,
                    maxLength: Int? = null) =
    EmailDataType.create("email", isNullable, minLength, maxLength, enum).value!!

  fun integerDataType(name: String= "integer",
                      isNullable: Boolean = false,
                      enum: List<BigDecimal?> = emptyList(),
                      minimum: BigDecimal? = null,
                      maximum: BigDecimal? = null,
                      exclusiveMinimum: Boolean = false,
                      exclusiveMaximum: Boolean = false) =
    IntegerDataType.create(name, isNullable, enum, minimum, maximum, exclusiveMinimum, exclusiveMaximum).value!!

  fun numberDataType(isNullable: Boolean = false,
                     enum: List<BigDecimal?> = emptyList(),
                     minimum: BigDecimal? = null,
                     maximum: BigDecimal? = null,
                     exclusiveMinimum: Boolean = false,
                     exclusiveMaximum: Boolean = false) =
    NumberDataType.create("number", isNullable, enum, minimum, maximum, exclusiveMinimum, exclusiveMaximum).value!!

  fun objectDataType(name: String = "object",
                     properties: Map<String, DataType<out Any>>,
                     requiredProperties: Set<String> = emptySet(),
                     isNullable: Boolean = false,
                     enum: List<Any?> = emptyList()) =
    ObjectDataType.create(name, properties, requiredProperties, isNullable, enum).value!!

  fun oneOfDataType(name: String ="oneOf",
                    subTypes: List<DataType<out Any>>,
                    discriminator: Discriminator? = null,
                    isNullable: Boolean = false,
                    enum: List<Any?> = emptyList()) =
    OneOfDataType.create(name, subTypes, discriminator, isNullable, enum).value!!

  fun stringDataType(name: String = "string",
                     isNullable: Boolean = false,
                     enum: List<String?> = emptyList(),
                     minLength: Int? = null,
                     maxLength: Int? = null) =
    StringDataType.create(name, "string", isNullable, enum, minLength, maxLength).value!!

  fun uuidDataType(isNullable: Boolean = false,
                   enum: List<String?> = emptyList()) =
    UuidDataType.create("uuid", isNullable, enum).value!!
}