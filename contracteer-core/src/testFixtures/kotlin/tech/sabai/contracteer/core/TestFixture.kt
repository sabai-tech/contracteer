package tech.sabai.contracteer.core

import tech.sabai.contracteer.core.Result.Failure
import tech.sabai.contracteer.core.Result.Success
import tech.sabai.contracteer.core.datatype.*
import java.math.BigDecimal

object TestFixture {

  fun allOfDataType(
    name: String = "allOf",
    subTypes: List<DataType<out Any>>,
    isNullable: Boolean = false,
    discriminator: Discriminator? = null,
    enum: List<Any?> = emptyList()) =
    AllOfDataType.create(name, subTypes, isNullable, discriminator, enum).assertSuccess()

  fun anyOfDataType(name: String = "anyOf",
                    subTypes: List<DataType<out Any>>,
                    discriminator: Discriminator? = null,
                    isNullable: Boolean = false,
                    enum: List<Any?> = emptyList()) =
    AnyOfDataType.create(name, subTypes, discriminator, isNullable, enum).assertSuccess()

  fun arrayDataType(itemDataType: DataType<out Any>,
                    isNullable: Boolean = false,
                    enum: List<Any?> = emptyList(),
                    minItems: Int? = null,
                    maxItems: Int? = null,
                    uniqueItems: Boolean = false) =
    ArrayDataType.create("array", itemDataType, isNullable, enum, minItems, maxItems, uniqueItems).assertSuccess()

  fun base64DataType(isNullable: Boolean = false,
                     enum: List<String?> = emptyList(),
                     minLength: Int? = null,
                     maxLength: Int? = null) =
    Base64DataType.create("base64", isNullable, enum, minLength, maxLength).assertSuccess()

  fun binaryDataType(isNullable: Boolean = false,
                     enum: List<String?> = emptyList(),
                     minLength: Int? = null,
                     maxLength: Int? = null) =
    BinaryDataType.create("binary", isNullable, enum, minLength, maxLength).assertSuccess()

  fun booleanDataType(isNullable: Boolean = false,
                      enum: List<Boolean?> = emptyList()) =
    BooleanDataType.create("boolean", isNullable, enum).assertSuccess()

  fun dateDataType(isNullable: Boolean = false,
                   enum: List<String?> = emptyList()) =
    DateDataType.create("date", isNullable, enum).assertSuccess()

  fun dateTimeDataType(isNullable: Boolean = false,
                       enum: List<String?> = emptyList()) =
    DateTimeDataType.create("dateTime", isNullable, enum).assertSuccess()

  fun emailDataType(isNullable: Boolean = false,
                    enum: List<String?> = emptyList(),
                    minLength: Int? = null,
                    maxLength: Int? = null) =
    EmailDataType.create("email", isNullable, minLength, maxLength, enum).assertSuccess()

  fun integerDataType(name: String = "integer",
                      isNullable: Boolean = false,
                      enum: List<BigDecimal?> = emptyList(),
                      minimum: BigDecimal? = null,
                      maximum: BigDecimal? = null,
                      exclusiveMinimum: Boolean = false,
                      exclusiveMaximum: Boolean = false,
                      multipleOf: BigDecimal? = null) =
    IntegerDataType.create(name, isNullable, enum, minimum, maximum, exclusiveMinimum, exclusiveMaximum, multipleOf).assertSuccess()

  fun numberDataType(isNullable: Boolean = false,
                     enum: List<BigDecimal?> = emptyList(),
                     minimum: BigDecimal? = null,
                     maximum: BigDecimal? = null,
                     exclusiveMinimum: Boolean = false,
                     exclusiveMaximum: Boolean = false,
                     multipleOf: BigDecimal? = null) =
    NumberDataType.create("number", isNullable, enum, minimum, maximum, exclusiveMinimum, exclusiveMaximum, multipleOf).assertSuccess()

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
    ObjectDataType.create(name, properties, requiredProperties, readOnlyProperties, writeOnlyProperties, allowAdditionalProperties, additionalPropertiesDataType, isNullable, enum, minProperties, maxProperties).assertSuccess()

  fun oneOfDataType(name: String = "oneOf",
                    subTypes: List<DataType<out Any>>,
                    discriminator: Discriminator? = null,
                    isNullable: Boolean = false,
                    enum: List<Any?> = emptyList()) =
    OneOfDataType.create(name, subTypes, discriminator, isNullable, enum).assertSuccess()

  fun stringDataType(name: String = "string",
                     isNullable: Boolean = false,
                     enum: List<String?> = emptyList(),
                     minLength: Int? = null,
                     maxLength: Int? = null,
                     pattern: String? = null) =
    StringDataType.create(name, "string", isNullable, enum, minLength, maxLength, pattern).assertSuccess()

  fun uuidDataType(isNullable: Boolean = false,
                   enum: List<String?> = emptyList()) =
    UuidDataType.create("uuid", isNullable, enum).assertSuccess()
}

// Test assertion helpers
fun <T> Result<T>.assertSuccess(): T = when (this) {
  is Success -> value
  is Failure -> throw AssertionError("Expected success but got errors: ${errors()}")
}

fun <T> Result<T>.assertFailure(): List<String> = when (this) {
  is Failure -> errors()
  is Success -> throw AssertionError("Expected failure but got success with value: $value")
}

fun <T> List<T>.assertSingle(): T {
  assert(size == 1) { "Expected single element but got $size" }
  return single()
}

// ParameterCodec test helpers
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

