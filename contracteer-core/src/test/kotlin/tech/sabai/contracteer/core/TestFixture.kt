package tech.sabai.contracteer.core

import tech.sabai.contracteer.core.datatype.*
import java.math.BigDecimal

object TestFixture {

  fun allOfDataType(
    name: String = "allOf",
    subTypes: List<DataType<Map<String, Any?>>>,
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
                    enum: List<Any?> = emptyList(),
                    minItems: Int? = null,
                    maxItems: Int? = null,
                    uniqueItems: Boolean = false) =
    ArrayDataType.create("array", itemDataType, isNullable, enum, minItems, maxItems, uniqueItems).value!!

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

  fun integerDataType(name: String = "integer",
                      isNullable: Boolean = false,
                      enum: List<BigDecimal?> = emptyList(),
                      minimum: BigDecimal? = null,
                      maximum: BigDecimal? = null,
                      exclusiveMinimum: Boolean = false,
                      exclusiveMaximum: Boolean = false,
                      multipleOf: BigDecimal? = null) =
    IntegerDataType.create(name, isNullable, enum, minimum, maximum, exclusiveMinimum, exclusiveMaximum, multipleOf).value!!

  fun numberDataType(isNullable: Boolean = false,
                     enum: List<BigDecimal?> = emptyList(),
                     minimum: BigDecimal? = null,
                     maximum: BigDecimal? = null,
                     exclusiveMinimum: Boolean = false,
                     exclusiveMaximum: Boolean = false,
                     multipleOf: BigDecimal? = null) =
    NumberDataType.create("number", isNullable, enum, minimum, maximum, exclusiveMinimum, exclusiveMaximum, multipleOf).value!!

  fun objectDataType(name: String = "object",
                     properties: Map<String, DataType<out Any>>,
                     requiredProperties: Set<String> = emptySet(),
                     readOnlyProperties: Set<String> = emptySet(),
                     writeOnlyProperties: Set<String> = emptySet(),
                     allowAdditionalProperties: Boolean = true,
                     additionalPropertiesDataType: DataType<out Any>? = null,
                     isNullable: Boolean = false,
                     enum: List<Any?> = emptyList(),
                     minProperties: Int? = null,
                     maxProperties: Int? = null) =
    ObjectDataType.create(name, properties, requiredProperties, readOnlyProperties, writeOnlyProperties, allowAdditionalProperties, additionalPropertiesDataType, isNullable, enum, minProperties, maxProperties).value!!

  fun oneOfDataType(name: String = "oneOf",
                    subTypes: List<DataType<out Any>>,
                    discriminator: Discriminator? = null,
                    isNullable: Boolean = false,
                    enum: List<Any?> = emptyList()) =
    OneOfDataType.create(name, subTypes, discriminator, isNullable, enum).value!!

  fun stringDataType(name: String = "string",
                     isNullable: Boolean = false,
                     enum: List<String?> = emptyList(),
                     minLength: Int? = null,
                     maxLength: Int? = null,
                     pattern: String? = null) =
    StringDataType.create(name, "string", isNullable, enum, minLength, maxLength, pattern).value!!

  fun uuidDataType(isNullable: Boolean = false,
                   enum: List<String?> = emptyList()) =
    UuidDataType.create("uuid", isNullable, enum).value!!
}

// Test assertion helpers
fun <T> Result<T>.assertSuccess(): T {
  assert(isSuccess()) { "Expected success but got errors: ${errors()}" }
  return value!!
}

fun <T> Result<T>.assertFailure(): List<String> {
  assert(isFailure()) { "Expected failure but got success with value: $value" }
  return errors()
}

fun <T> List<T>.assertSingle(): T {
  assert(size == 1) { "Expected single element but got $size" }
  return single()
}

// StyleCodec test helpers
fun valueExtractor(vararg entries: Pair<String, List<String>>): (String) -> List<String> {
  val map = entries.toMap()
  return { key -> map[key] ?: emptyList() }
}

fun rgbObjectDataType() = TestFixture.objectDataType(
  properties = mapOf(
    "R" to TestFixture.integerDataType(),
    "G" to TestFixture.integerDataType(),
    "B" to TestFixture.integerDataType()
  )
)

