package tech.sabai.contracteer.core.dsl

import java.math.BigDecimal
import tech.sabai.contracteer.core.assertSuccess
import tech.sabai.contracteer.core.datatype.AllOfDataType
import tech.sabai.contracteer.core.datatype.AnyOfDataType
import tech.sabai.contracteer.core.datatype.ArrayDataType
import tech.sabai.contracteer.core.datatype.Base64DataType
import tech.sabai.contracteer.core.datatype.BinaryDataType
import tech.sabai.contracteer.core.datatype.BooleanDataType
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.datatype.DateDataType
import tech.sabai.contracteer.core.datatype.DateTimeDataType
import tech.sabai.contracteer.core.datatype.Discriminator
import tech.sabai.contracteer.core.datatype.EmailDataType
import tech.sabai.contracteer.core.datatype.IntegerDataType
import tech.sabai.contracteer.core.datatype.NumberDataType
import tech.sabai.contracteer.core.datatype.ObjectDataType
import tech.sabai.contracteer.core.datatype.OneOfDataType
import tech.sabai.contracteer.core.datatype.StringDataType
import tech.sabai.contracteer.core.datatype.UuidDataType

// === Scalar factories ===

fun stringType(
  name: String = "string",
  isNullable: Boolean = false,
  enum: List<String?> = emptyList(),
  minLength: Int? = null,
  maxLength: Int? = null,
  pattern: String? = null
): StringDataType =
  StringDataType.create(name, "string", isNullable, enum, minLength, maxLength, pattern).assertSuccess()

fun integerType(
  name: String = "integer",
  isNullable: Boolean = false,
  enum: List<BigDecimal?> = emptyList(),
  minimum: BigDecimal? = null,
  maximum: BigDecimal? = null,
  exclusiveMinimum: Boolean = false,
  exclusiveMaximum: Boolean = false,
  multipleOf: BigDecimal? = null
): IntegerDataType =
  IntegerDataType.create(name, isNullable, enum, minimum, maximum, exclusiveMinimum, exclusiveMaximum, multipleOf).assertSuccess()

fun numberType(
  name: String = "number",
  isNullable: Boolean = false,
  enum: List<BigDecimal?> = emptyList(),
  minimum: BigDecimal? = null,
  maximum: BigDecimal? = null,
  exclusiveMinimum: Boolean = false,
  exclusiveMaximum: Boolean = false,
  multipleOf: BigDecimal? = null
): NumberDataType =
  NumberDataType.create(name, isNullable, enum, minimum, maximum, exclusiveMinimum, exclusiveMaximum, multipleOf).assertSuccess()

fun booleanType(
  isNullable: Boolean = false,
  enum: List<Boolean?> = emptyList()
): BooleanDataType =
  BooleanDataType.create("boolean", isNullable, enum).assertSuccess()

fun dateType(
  isNullable: Boolean = false,
  enum: List<String?> = emptyList()
): DateDataType =
  DateDataType.create("date", isNullable, enum).assertSuccess()

fun dateTimeType(
  isNullable: Boolean = false,
  enum: List<String?> = emptyList()
): DateTimeDataType =
  DateTimeDataType.create("dateTime", isNullable, enum).assertSuccess()

fun uuidType(
  isNullable: Boolean = false,
  enum: List<String?> = emptyList()
): UuidDataType =
  UuidDataType.create("uuid", isNullable, enum).assertSuccess()

fun emailType(
  isNullable: Boolean = false,
  enum: List<String?> = emptyList(),
  minLength: Int? = null,
  maxLength: Int? = null
): EmailDataType =
  EmailDataType.create("email", isNullable, minLength, maxLength, enum).assertSuccess()

fun base64Type(
  isNullable: Boolean = false,
  enum: List<String?> = emptyList(),
  minLength: Int? = null,
  maxLength: Int? = null
): Base64DataType =
  Base64DataType.create("base64", isNullable, enum, minLength, maxLength).assertSuccess()

fun binaryType(
  isNullable: Boolean = false,
  enum: List<String?> = emptyList(),
  minLength: Int? = null,
  maxLength: Int? = null
): BinaryDataType =
  BinaryDataType.create("binary", isNullable, enum, minLength, maxLength).assertSuccess()

fun arrayType(
  items: DataType<out Any>,
  isNullable: Boolean = false,
  enum: List<Any?> = emptyList(),
  minItems: Int? = null,
  maxItems: Int? = null,
  uniqueItems: Boolean = false
): ArrayDataType =
  ArrayDataType.create("array", items, isNullable, enum, minItems, maxItems, uniqueItems).assertSuccess()

// === Block builders ===

fun objectType(
  name: String = "object",
  isNullable: Boolean = false,
  allowAdditionalProperties: Boolean = true,
  additionalPropertiesDataType: DataType<out Any>? = null,
  readOnlyProperties: Set<String> = emptySet(),
  writeOnlyProperties: Set<String> = emptySet(),
  enum: List<Any?> = emptyList(),
  minProperties: Int? = null,
  maxProperties: Int? = null,
  block: ObjectTypeBuilder.() -> Unit = {}
): ObjectDataType {
  val builder = ObjectTypeBuilder().apply(block)
  return ObjectDataType.create(
    name = name,
    properties = builder.properties,
    requiredProperties = builder.required,
    readOnlyProperties = readOnlyProperties,
    writeOnlyProperties = writeOnlyProperties,
    allowAdditionalProperties = allowAdditionalProperties,
    additionalPropertiesDataType = additionalPropertiesDataType,
    isNullable = isNullable,
    enum = enum,
    minProperties = minProperties,
    maxProperties = maxProperties
  ).assertSuccess()
}

fun allOfType(
  name: String = "allOf",
  isNullable: Boolean = false,
  enum: List<Any?> = emptyList(),
  block: CompositeTypeBuilder.() -> Unit = {}
): AllOfDataType {
  val builder = CompositeTypeBuilder().apply(block)
  return AllOfDataType.create(name, builder.subTypes, isNullable, builder.discriminator, enum).assertSuccess()
}

fun oneOfType(
  name: String = "oneOf",
  isNullable: Boolean = false,
  enum: List<Any?> = emptyList(),
  block: CompositeTypeBuilder.() -> Unit = {}
): OneOfDataType {
  val builder = CompositeTypeBuilder().apply(block)
  return OneOfDataType.create(name, builder.subTypes, builder.discriminator, isNullable, enum).assertSuccess()
}

fun anyOfType(
  name: String = "anyOf",
  isNullable: Boolean = false,
  enum: List<Any?> = emptyList(),
  block: CompositeTypeBuilder.() -> Unit = {}
): AnyOfDataType {
  val builder = CompositeTypeBuilder().apply(block)
  return AnyOfDataType.create(name, builder.subTypes, builder.discriminator, isNullable, enum).assertSuccess()
}

// === Sub-builders ===

@TestBuilder
class ObjectTypeBuilder internal constructor() {
  internal val properties = mutableMapOf<String, DataType<out Any>>()
  internal val required = mutableSetOf<String>()

  fun properties(block: PropertiesBuilder.() -> Unit) {
    properties.putAll(PropertiesBuilder().apply(block).entries)
  }

  fun required(vararg names: String) {
    required.addAll(names)
  }
}

@TestBuilder
class PropertiesBuilder internal constructor() {
  internal val entries = mutableMapOf<String, DataType<out Any>>()

  infix fun String.to(dataType: DataType<out Any>) {
    entries[this] = dataType
  }
}

@TestBuilder
class CompositeTypeBuilder internal constructor() {
  internal val subTypes = mutableListOf<DataType<out Any>>()
  internal var discriminator: Discriminator? = null

  fun subType(dataType: DataType<out Any>) {
    subTypes += dataType
  }

  fun discriminator(propertyName: String, block: DiscriminatorBuilder.() -> Unit = {}) {
    discriminator = Discriminator(propertyName, DiscriminatorBuilder().apply(block).mappings)
  }
}

@TestBuilder
class DiscriminatorBuilder internal constructor() {
  internal val mappings = mutableMapOf<String, String>()

  fun mapping(discriminatorValue: String, schemaName: String) {
    mappings[discriminatorValue] = schemaName
  }
}
