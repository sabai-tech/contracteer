package tech.sabai.contracteer.core.swagger

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import tech.sabai.contracteer.core.assertFailure
import tech.sabai.contracteer.core.assertSuccess
import tech.sabai.contracteer.core.datatype.*

class SchemaConversionTest {

  @ParameterizedTest(name = "extract IntegerDataType (OAS {0})")
  @ValueSource(strings = ["3.0", "3.1"])
  fun `extract IntegerDataType`(version: String) {
    // when
    val integerDataType = getDataType(version, "integer.yaml") as IntegerDataType

    // then
    assert(integerDataType.allowedValues != null)
    assert(integerDataType.allowedValues!!.contains(10).isSuccess())
    assert(integerDataType.allowedValues.contains(20).isSuccess())
    assert(integerDataType.range.minimum == 9.toBigDecimal())
    assert(integerDataType.range.maximum == 20.toBigDecimal())
    assert(integerDataType.range.exclusiveMinimum)
    assert(integerDataType.range.exclusiveMaximum.not())
  }

  @ParameterizedTest(name = "extract NumberDataType (OAS {0})")
  @ValueSource(strings = ["3.0", "3.1"])
  fun `extract NumberDataType`(version: String) {
    // when
    val numberDataType = getDataType(version, "number.yaml") as NumberDataType

    // then
    assert(numberDataType.allowedValues != null)
    assert(numberDataType.allowedValues!!.contains(10.5).isSuccess())
    assert(numberDataType.allowedValues.contains(20).isSuccess())
    assert(numberDataType.range.minimum == 10.toBigDecimal())
    assert(numberDataType.range.maximum == 20.3.toBigDecimal())
    assert(numberDataType.range.exclusiveMinimum)
    assert(numberDataType.range.exclusiveMaximum.not())
  }

  @Test
  fun `extract NumberDataType without type property`() {
    // when
    val numberDataType = getDataType("3.0", "number_without_type.yaml") as NumberDataType

    // then
    assert(numberDataType.allowedValues != null)
    assert(numberDataType.allowedValues!!.contains(10.5).isSuccess())
    assert(numberDataType.allowedValues.contains(20).isSuccess())
    assert(numberDataType.range.minimum == 10.toBigDecimal())
    assert(numberDataType.range.maximum == 20.3.toBigDecimal())
    assert(numberDataType.range.exclusiveMinimum)
    assert(numberDataType.range.exclusiveMaximum.not())
  }

  @ParameterizedTest(name = "extract StringDataType (OAS {0})")
  @ValueSource(strings = ["3.0", "3.1"])
  fun `extract StringDataType`(version: String) {
    // when
    val stringDataType = getDataType(version, "string.yaml") as StringDataType

    // then
    assert(stringDataType.allowedValues != null)
    assert(stringDataType.allowedValues!!.contains("cat").isSuccess())
    assert(stringDataType.allowedValues.contains("dog").isSuccess())
    assert(stringDataType.lengthRange.minimum == 2.toBigDecimal())
    assert(stringDataType.lengthRange.maximum == 10.toBigDecimal())
    assert(stringDataType.lengthRange.exclusiveMinimum.not())
    assert(stringDataType.lengthRange.exclusiveMaximum.not())
  }

  @Test
  fun `extract StringDataType without type property`() {
    // when
    val stringDataType = getDataType("3.0", "string_without_type.yaml") as StringDataType

    // then
    assert(stringDataType.allowedValues != null)
    assert(stringDataType.allowedValues!!.contains("cat").isSuccess())
    assert(stringDataType.allowedValues.contains("dog").isSuccess())
    assert(stringDataType.lengthRange.minimum == 2.toBigDecimal())
    assert(stringDataType.lengthRange.maximum == 10.toBigDecimal())
    assert(stringDataType.lengthRange.exclusiveMinimum.not())
    assert(stringDataType.lengthRange.exclusiveMaximum.not())
  }

  @ParameterizedTest(name = "extract Base64DataType (OAS {0})")
  @ValueSource(strings = ["3.0", "3.1"])
  fun `extract Base64DataType`(version: String) {
    // when
    val base64DataType = getDataType(version, "string_base64.yaml") as Base64DataType

    // then
    assert(base64DataType.allowedValues != null)
    assert(base64DataType.allowedValues!!.contains("Y2F0").isSuccess())
    assert(base64DataType.allowedValues.contains("ZG9n").isSuccess())
    assert(base64DataType.lengthRange.minimum == 4.toBigDecimal())
    assert(base64DataType.lengthRange.maximum == 12.toBigDecimal())
    assert(base64DataType.lengthRange.exclusiveMinimum.not())
    assert(base64DataType.lengthRange.exclusiveMaximum.not())
  }

  @Test
  fun `extract Base64DataType from contentEncoding`() {
    // when
    val base64DataType = getDataType("3.1", "string_base64_content_encoding.yaml") as Base64DataType

    // then
    assert(base64DataType.allowedValues != null)
    assert(base64DataType.allowedValues!!.contains("Y2F0").isSuccess())
    assert(base64DataType.allowedValues.contains("ZG9n").isSuccess())
    assert(base64DataType.lengthRange.minimum == 4.toBigDecimal())
    assert(base64DataType.lengthRange.maximum == 12.toBigDecimal())
  }

  @Test
  fun `rejects schema with non-base64 contentEncoding`() {
    // when
    val result = loadOperations("3.1", "non_base64_content_encoding_error.yaml")

    // then
    val errors = result.assertFailure()
    assert(errors.any { it.contains("contentEncoding") && it.contains("base16") })
    assert(errors.any { it.contains("Only 'base64' is supported") })
  }

  @ParameterizedTest(name = "extract BinaryDataType (OAS {0})")
  @ValueSource(strings = ["3.0", "3.1"])
  fun `extract BinaryDataType`(version: String) {
    // when
    val binaryDataType = getDataType(version, "string_binary.yaml") as BinaryDataType

    // then
    assert(binaryDataType.allowedValues != null)
    assert(binaryDataType.allowedValues!!.contains("ÔSÌì&").isSuccess())
    assert(binaryDataType.allowedValues.contains("Çþ}OZ").isSuccess())
    assert(binaryDataType.lengthRange.minimum == 2.toBigDecimal())
    assert(binaryDataType.lengthRange.maximum == 10.toBigDecimal())
    assert(binaryDataType.lengthRange.exclusiveMinimum.not())
    assert(binaryDataType.lengthRange.exclusiveMaximum.not())
  }

  @Test
  fun `extract BinaryDataType from contentMediaType`() {
    // when
    val binaryDataType = getDataType("3.1", "string_binary_content_media_type.yaml") as BinaryDataType

    // then
    assert(binaryDataType.allowedValues != null)
    assert(binaryDataType.allowedValues!!.contains("bytes1").isSuccess())
    assert(binaryDataType.allowedValues.contains("bytes2").isSuccess())
    assert(binaryDataType.lengthRange.minimum == 4.toBigDecimal())
    assert(binaryDataType.lengthRange.maximum == 12.toBigDecimal())
  }

  @Test
  fun `rejects 3 1 string schema with structured-text contentMediaType`() {
    // when
    val result = loadOperations("3.1", "structured_text_content_media_type_error.yaml")

    // then
    val errors = result.assertFailure()
    assert(errors.any { it.contains("contentMediaType") && it.contains("application/json") })
    assert(errors.any { it.contains("not yet supported") })
  }

  @Test
  fun `ignores contentMediaType annotation on a non-string schema`() {
    // when
    val operations = loadOperations("3.1", "object_with_content_media_type_annotation.yaml").assertSuccess()

    // then
    val dataType = operations.first().requestSchema.bodies.first().dataType
    assert(dataType is ObjectDataType)
  }

  @ParameterizedTest(name = "extract BinaryDataType from empty schema under octet-stream content type (OAS {0})")
  @ValueSource(strings = ["3.0", "3.1"])
  fun `extract BinaryDataType from empty schema under octet-stream content type`(version: String) {
    // when
    val operations = loadOperations(version, "binary_empty_schema_under_octet_stream.yaml").assertSuccess()

    // then
    val dataType = operations.first().requestSchema.bodies.first().dataType
    assert(dataType is BinaryDataType)
  }

  @Test
  fun `extract BinaryDataType from empty schema under image content type`() {
    // when
    val operations = loadOperations("3.1", "binary_empty_schema_under_image_png.yaml").assertSuccess()

    // then
    val dataType = operations.first().requestSchema.bodies.first().dataType
    assert(dataType is BinaryDataType)
  }

  @ParameterizedTest(name = "extract UuidDataType (OAS {0})")
  @ValueSource(strings = ["3.0", "3.1"])
  fun `extract UuidDataType`(version: String) {
    // when
    val uuidDataType = getDataType(version, "string_uuid.yaml") as UuidDataType

    // then
    assert(uuidDataType.allowedValues != null)
    assert(uuidDataType.allowedValues!!.contains("d972d2c3-9b84-4076-a836-aa2465acd9fb").isSuccess())
    assert(uuidDataType.allowedValues.contains("24b12872-6410-46c5-81d3-c589e849dfca").isSuccess())
  }

  @Test
  fun `rejects UUID schema when enum contains a non-string value`() {
    // When
    val result = loadOperations("3.1", "string_uuid_invalid_enum_error.yaml")

    // Then
    val errors = result.assertFailure()
    assert(errors.any { "42" in it }) { "Expected an error mentioning the invalid value '42' but got: $errors" }
  }

  @ParameterizedTest(name = "extract EmailDataType (OAS {0})")
  @ValueSource(strings = ["3.0", "3.1"])
  fun `extract EmailDataType`(version: String) {
    // when
    val emailDataType = getDataType(version, "string_email.yaml") as EmailDataType

    // then
    assert(emailDataType.allowedValues != null)
    assert(emailDataType.allowedValues!!.contains("john@example.com").isSuccess())
    assert(emailDataType.allowedValues.contains("jane@example.com").isSuccess())
    assert(emailDataType.lengthRange.minimum == 6.toBigDecimal())
    assert(emailDataType.lengthRange.maximum == 100.toBigDecimal())
    assert(emailDataType.lengthRange.exclusiveMinimum.not())
    assert(emailDataType.lengthRange.exclusiveMaximum.not())
  }

  @ParameterizedTest(name = "extract DateDataType (OAS {0})")
  @ValueSource(strings = ["3.0", "3.1"])
  fun `extract DateDataType`(version: String) {
    // when
    val dateDataType = getDataType(version, "string_date.yaml") as DateDataType

    // then
    assert(dateDataType.allowedValues != null)
    assert(dateDataType.allowedValues!!.contains("2020-12-01").isSuccess())
    assert(dateDataType.allowedValues.contains("2024-01-01").isSuccess())
  }

  @ParameterizedTest(name = "extract DateTimeDataType (OAS {0})")
  @ValueSource(strings = ["3.0", "3.1"])
  fun `extract DateTimeDataType`(version: String) {
    // when
    val dateTimeDataType = getDataType(version, "string_datetime.yaml") as DateTimeDataType

    // then
    assert(dateTimeDataType.allowedValues != null)
    assert(dateTimeDataType.allowedValues!!.contains("2020-12-20T15:30:45+02:00").isSuccess())
    assert(dateTimeDataType.allowedValues.contains("2024-12-20T15:30:45+02:00").isSuccess())
  }

  @ParameterizedTest(name = "extract BooleanDataType (OAS {0})")
  @ValueSource(strings = ["3.0", "3.1"])
  fun `extract BooleanDataType`(version: String) {
    // when
    val booleanDataType = getDataType(version, "boolean.yaml") as BooleanDataType

    // then
    assert(booleanDataType.allowedValues != null)
    assert(booleanDataType.allowedValues!!.contains(false).isSuccess())
  }

  @Test
  fun `extract BooleanDataType without type property`() {
    // when
    val booleanDataType = getDataType("3.0", "boolean_without_type.yaml") as BooleanDataType

    // then
    assert(booleanDataType.allowedValues != null)
    assert(booleanDataType.allowedValues!!.contains(false).isSuccess())
  }

  @ParameterizedTest(name = "extract ArrayDataType (OAS {0})")
  @ValueSource(strings = ["3.0", "3.1"])
  fun `extract ArrayDataType`(version: String) {
    // when
    val arrayDataType = getDataType(version, "array.yaml") as ArrayDataType

    // then
    assert(arrayDataType.itemDataType is StringDataType)
    assert(arrayDataType.allowedValues != null)
    assert(arrayDataType.allowedValues!!.contains(listOf("cat", "dog")).isSuccess())
    assert(arrayDataType.allowedValues.contains(listOf("john", "jane")).isSuccess())
  }

  @ParameterizedTest(name = "extract ObjectDataType (OAS {0})")
  @ValueSource(strings = ["3.0", "3.1"])
  fun `extract ObjectDataType`(version: String) {
    // when
    val objectDataType = getDataType(version, "object.yaml") as ObjectDataType

    // then
    assert(objectDataType.properties.keys == setOf("name", "age"))
    assert(objectDataType.properties["name"]!! is StringDataType)
    assert(objectDataType.properties["age"]!! is IntegerDataType)
    assert(objectDataType.requiredProperties == setOf("name"))
    assert(!objectDataType.allowAdditionalProperties)
    assert(objectDataType.additionalPropertiesDataType == null)
    assert(objectDataType.allowedValues != null)
    assert(objectDataType.allowedValues!!.contains(mapOf("name" to "john", "age" to 30)).isSuccess())
    assert(objectDataType.allowedValues.contains(mapOf("name" to "jane")).isSuccess())
  }

  @Test
  fun `extract ObjectDataType without type property`() {
    // when
    val objectDataType = getDataType("3.0", "object_without_type.yaml") as ObjectDataType

    // then
    assert(objectDataType.properties.keys == setOf("name", "age"))
    assert(objectDataType.properties["name"]!! is StringDataType)
    assert(objectDataType.properties["age"]!! is IntegerDataType)
    assert(objectDataType.requiredProperties == setOf("name"))
    assert(objectDataType.allowedValues != null)
    assert(objectDataType.allowedValues!!.contains(mapOf("name" to "john", "age" to 30)).isSuccess())
    assert(objectDataType.allowedValues.contains(mapOf("name" to "jane")).isSuccess())
  }

  @Test
  fun `extract ObjectDataType with additional properties`() {
    // when
    val objectDataType = getDataType("3.0", "object_additional_properties.yaml") as ObjectDataType

    // then
    assert(objectDataType.properties.keys == setOf("name", "age"))
    assert(objectDataType.properties["name"]!! is StringDataType)
    assert(objectDataType.properties["age"]!! is IntegerDataType)
    assert(objectDataType.allowAdditionalProperties)
    assert(objectDataType.additionalPropertiesDataType is StringDataType)
  }

  @ParameterizedTest(name = "extract AnyOfDataType (OAS {0})")
  @ValueSource(strings = ["3.0", "3.1"])
  fun `extract AnyOfDataType`(version: String) {
    // when
    val anyOfDataType = getDataType(version, "anyOf.yaml") as AnyOfDataType

    // then
    assert(anyOfDataType.subTypes.all { it is StringDataType || it is IntegerDataType || it is ObjectDataType })
    assert(anyOfDataType.allowedValues!!.contains("Hello").isSuccess())
    assert(anyOfDataType.allowedValues.contains(42).isSuccess())
    assert(anyOfDataType.allowedValues.contains(mapOf("name" to "john", "age" to 42)).isSuccess())
    assert(anyOfDataType.allowedValues.contains(mapOf("prop1" to "Yo")).isSuccess())
  }

  @Test
  fun `extract AnyOfDataType with discriminator`() {
    // when
    val anyOfDataType = getDataType("3.0", "anyOf_discriminator.yaml") as AnyOfDataType

    // then
    assert(anyOfDataType.subTypes.size == 2)
    assert(anyOfDataType.subTypes.all { it.name == "cat" || it.name == "dog" })
    assert(anyOfDataType.discriminator == Discriminator("type", mapOf("DOG" to "dog")))
  }

  @ParameterizedTest(name = "extract OneOfDataType (OAS {0})")
  @ValueSource(strings = ["3.0", "3.1"])
  fun `extract OneOfDataType`(version: String) {
    // when
    val oneOfDataType = getDataType(version, "oneOf.yaml") as OneOfDataType

    // then
    assert(oneOfDataType.subTypes.all { it is StringDataType || it is IntegerDataType || it is ObjectDataType })
    assert(oneOfDataType.allowedValues!!.contains("Hello").isSuccess())
    assert(oneOfDataType.allowedValues.contains(42).isSuccess())
    assert(oneOfDataType.allowedValues.contains(mapOf("name" to "john", "age" to 42)).isSuccess())
    assert(oneOfDataType.allowedValues.contains(mapOf("prop1" to "Yo")).isSuccess())
  }

  @Test
  fun `extract OneOfDataType with discriminator`() {
    // when
    val oneOfDataType = getDataType("3.0", "oneOf_discriminator.yaml") as OneOfDataType

    // then
    assert(oneOfDataType.subTypes.size == 2)
    assert(oneOfDataType.subTypes.all { it.name == "cat" || it.name == "dog" })
    assert(oneOfDataType.discriminator == Discriminator("type", mapOf("DOG" to "dog")))
  }

  @ParameterizedTest(name = "extract AllOfDataType (OAS {0})")
  @ValueSource(strings = ["3.0", "3.1"])
  fun `extract AllOfDataType`(version: String) {
    // when
    val allOfDataType = getDataType(version, "allOf.yaml") as AllOfDataType

    // then
    assert(allOfDataType.subTypes.size == 2)
    assert(allOfDataType.subTypes.all { it.name == "pet" || it.name == "allOf #0" })
    assert(allOfDataType.allowedValues!!.contains(mapOf("name" to "kitty", "age" to 3)).isSuccess())
    assert(allOfDataType.allowedValues.contains(mapOf("name" to "medor", "age" to 4)).isSuccess())
  }

  @Test
  fun `extract AllOfDataType with discriminator`() {
    // when
    val allOfDataType = getDataType("3.0", "allOf_discriminator.yaml") as AllOfDataType

    // then
    assert(allOfDataType.subTypes.size == 2)
    assert(allOfDataType.subTypes.all { it.name == "Pet" || it.name == "allOf #1" })
    assert(allOfDataType.discriminator == Discriminator("petType", mapOf("dog" to "Dog")))
  }

  @ParameterizedTest(name = "extract AnyDataType (OAS {0})")
  @ValueSource(strings = ["3.0", "3.1"])
  fun `extract AnyDataType`(version: String) {
    // when
    val anyDataType = getDataType(version, "any.yaml")

    // then
    assert(anyDataType is AnyDataType)
  }

  @Test
  fun `does not extract AllOfDataType when there are multiple discriminators`() {
    // when
    val result = loadOperations("3.0", "allOf_multiple_discriminators_error.yaml")

    // then
    result.assertFailure()
  }

  @Test
  fun `does not extract schema when multiple composition keywords are present`() {
    // when
    val result = loadOperations("3.0", "multiple_composition_keywords_error.yaml")

    // then
    result.assertFailure()
  }

  @Test
  fun `extract AllOfDataType with sibling properties folded into sub-types`() {
    // when
    val allOfDataType = getDataType("3.0", "allOf_sibling_properties.yaml") as AllOfDataType

    // then
    assert(allOfDataType.subTypes.size == 2)
    assert(allOfDataType.subTypes.any { it.name == "Pet" })
    val value = allOfDataType.randomValue()
    assert(allOfDataType.validate(value).isSuccess())
    assert(allOfDataType.validate(mapOf("name" to "Kitty")).isFailure()) // missing huntingSkill
    assert(allOfDataType.validate(mapOf("huntingSkill" to "lazy")).isFailure()) // missing name
    assert(allOfDataType.validate(mapOf("name" to "Kitty", "huntingSkill" to "lazy")).isSuccess())
  }

  @Test
  fun `extract AllOfDataType wrapping oneOf when oneOf has sibling properties`() {
    // when
    val dataType = getDataType("3.0", "oneOf_sibling_properties.yaml")

    // then
    assert(dataType is AllOfDataType)
    val allOfDataType = dataType as AllOfDataType
    assert(allOfDataType.subTypes.size == 2)
    assert(allOfDataType.subTypes.any { it is OneOfDataType })
    assert(allOfDataType.validate(mapOf("name" to "Kitty", "huntingSkill" to "lazy")).isSuccess())
    assert(allOfDataType.validate(mapOf("huntingSkill" to "lazy")).isFailure()) // missing sibling required 'name'
    assert(allOfDataType.validate(mapOf("name" to "Kitty")).isFailure()) // matches neither Cat nor Dog
  }

  @Test
  fun `extract AllOfDataType wrapping anyOf when anyOf has sibling properties`() {
    // when
    val dataType = getDataType("3.0", "anyOf_sibling_properties.yaml")

    // then
    assert(dataType is AllOfDataType)
    val allOfDataType = dataType as AllOfDataType
    assert(allOfDataType.subTypes.size == 2)
    assert(allOfDataType.subTypes.any { it is AnyOfDataType })
    assert(allOfDataType.validate(mapOf("name" to "Kitty", "huntingSkill" to "lazy")).isSuccess())
    assert(allOfDataType.validate(mapOf("huntingSkill" to "lazy")).isFailure()) // missing sibling required 'name'
  }

  @Test
  fun `extract AllOfDataType with single primitive sub-type`() {
    // when
    val allOfDataType = getDataType("3.0", "allOf_single_primitive.yaml") as AllOfDataType

    // then
    assert(allOfDataType.subTypes.size == 1)
    assert(allOfDataType.subTypes.first() is StringDataType)
    assert(allOfDataType.validate("hello").isSuccess())
    assert(allOfDataType.validate(42).isFailure())
    assert(allOfDataType.validate(allOfDataType.randomValue()).isSuccess())
  }

  @Test
  fun `extract AllOfDataType with inheritance`() {
    // when
    val operations = loadOperations("3.0", "allOf_inheritance.yaml").assertSuccess()

    // then
    val bodyDataType = operations.first().requestSchema.bodies.first().dataType
    assert(bodyDataType is AnyOfDataType)
    val anyOfDataType = bodyDataType as AnyOfDataType
    assert(anyOfDataType.subTypes.size == 3)
    assert(anyOfDataType.subTypes.all { it.name == "Dog" || it.name == "Cat" || it.name == "Lizard" })
    assert(anyOfDataType.subTypes.all { it is AllOfDataType })
  }

  @Test
  fun `extract IntegerDataType with int32 format applies 32-bit range`() {
    // when
    val dataType = getDataType("3.0", "integer_format.yaml", "int32_prop") as IntegerDataType

    // then
    assert(dataType.range.minimum == Int.MIN_VALUE.toBigDecimal())
    assert(dataType.range.maximum == Int.MAX_VALUE.toBigDecimal())
  }

  @Test
  fun `extract IntegerDataType with int64 format applies 64-bit range`() {
    // when
    val dataType = getDataType("3.0", "integer_format.yaml", "int64_prop") as IntegerDataType

    // then
    assert(dataType.range.minimum == Long.MIN_VALUE.toBigDecimal())
    assert(dataType.range.maximum == Long.MAX_VALUE.toBigDecimal())
  }

  @Test
  fun `extract IntegerDataType with int32 format narrows to explicit range when explicit is narrower`() {
    // when
    val dataType = getDataType("3.0", "integer_int32_with_range.yaml") as IntegerDataType

    // then — explicit range [-100, 100] is narrower than int32, so it wins
    assert(dataType.range.minimum == (-100).toBigDecimal())
    assert(dataType.range.maximum == 100.toBigDecimal())
  }

  @Test
  fun `rejects IntegerDataType with int32 format when explicit range exceeds format`() {
    // when
    val result = loadOperations("3.0", "integer_int32_with_wider_range.yaml")

    // then
    assert(result.isFailure())
    assert(result.errors().any { it.contains("out of range for format") })
  }

  @Test
  fun `extract IntegerDataType without format has no implicit range`() {
    // when
    val dataType = getDataType("3.0", "integer.yaml") as IntegerDataType

    // then — range comes from explicit min/max in the YAML, not from format
    assert(dataType.range.minimum == 9.toBigDecimal())
    assert(dataType.range.maximum == 20.toBigDecimal())
  }

  @Test
  fun `extract NumberDataType with float format applies float range`() {
    // when
    val dataType = getDataType("3.0", "number_format.yaml", "float_prop") as NumberDataType

    // then
    assert(dataType.range.minimum == Float.MAX_VALUE.toBigDecimal().negate())
    assert(dataType.range.maximum == Float.MAX_VALUE.toBigDecimal())
  }

  @Test
  fun `extract NumberDataType with float format narrows to explicit range when explicit is narrower`() {
    // when
    val dataType = getDataType("3.0", "number_float_with_range.yaml") as NumberDataType

    // then
    assert(dataType.range.minimum == (-100).toBigDecimal())
    assert(dataType.range.maximum == 100.toBigDecimal())
  }

  @Test
  fun `rejects NumberDataType with float format when explicit range exceeds format`() {
    // when
    val result = loadOperations("3.0", "number_float_with_wider_range.yaml")

    // then
    assert(result.isFailure())
    assert(result.errors().any { it.contains("out of range for format") })
  }

  @Test
  fun `extract NumberDataType with double format applies double range`() {
    // when
    val dataType = getDataType("3.0", "number_format.yaml", "double_prop") as NumberDataType

    // then
    assert(dataType.range.minimum == Double.MAX_VALUE.toBigDecimal().negate())
    assert(dataType.range.maximum == Double.MAX_VALUE.toBigDecimal())
  }

  @Test
  fun `extract ArrayDataType with minItems`() {
    // when
    val dataType = getDataType("3.0", "array_constraints.yaml", "with_min") as ArrayDataType

    // then
    assert(dataType.minItems == 1)
  }

  @Test
  fun `extract ArrayDataType with maxItems`() {
    // when
    val dataType = getDataType("3.0", "array_constraints.yaml", "with_max") as ArrayDataType

    // then
    assert(dataType.maxItems == 10)
  }

  @Test
  fun `extract ArrayDataType with minItems and maxItems`() {
    // when
    val dataType = getDataType("3.0", "array_constraints.yaml", "with_min_and_max") as ArrayDataType

    // then
    assert(dataType.minItems == 2)
    assert(dataType.maxItems == 5)
  }

  @Test
  fun `extract ArrayDataType with uniqueItems`() {
    // when
    val dataType = getDataType("3.0", "array_constraints.yaml", "with_unique") as ArrayDataType

    // then
    assert(dataType.uniqueItems)
  }

  @Test
  fun `extract IntegerDataType with multipleOf`() {
    // when
    val dataType = getDataType("3.0", "multiple_of.yaml", "integer_prop") as IntegerDataType

    // then
    assert(dataType.multipleOf == 5.toBigDecimal())
  }

  @Test
  fun `extract NumberDataType with multipleOf`() {
    // when
    val dataType = getDataType("3.0", "multiple_of.yaml", "number_prop") as NumberDataType

    // then
    assert(dataType.multipleOf == 0.01.toBigDecimal())
  }

  @Test
  fun `extract ObjectDataType with minProperties and maxProperties`() {
    // when
    val dataType = getDataType("3.0", "object_property_count.yaml") as ObjectDataType

    // then
    assert(dataType.minProperties == 1)
    assert(dataType.maxProperties == 5)
  }

  // --- const keyword (OAS 3.1) ---

  @ParameterizedTest(name = "extract DataType with const ({0})")
  @MethodSource("constFixtures")
  fun `extract DataType with const`(fixture: String, allowedValue: Any, disallowedValue: Any) {
    // when
    val dataType = getDataType("3.1", fixture)

    // then
    assert(dataType.allowedValues != null)
    assert(dataType.allowedValues!!.contains(allowedValue).isSuccess())
    assert(dataType.allowedValues!!.contains(disallowedValue).isFailure())
  }

  @Test
  fun `extract DataType when const is consistent with enum`() {
    // when
    val stringDataType = getDataType("3.1", "const_consistent_with_enum.yaml") as StringDataType

    // then
    assert(stringDataType.allowedValues != null)
    assert(stringDataType.allowedValues!!.contains("active").isSuccess())
  }

  @Test
  fun `rejects schema when const conflicts with enum`() {
    // when
    val result = loadOperations("3.1", "const_conflicts_with_enum_error.yaml")

    // then
    val errors = result.assertFailure()
    assert(errors.any { it.contains("const") && it.contains("enum") }) {
      "Expected an error mentioning conflict between const and enum but got: $errors"
    }
  }

  // --- propertyNames keyword (OAS 3.1) ---

  @Test
  fun `extract ObjectDataType with propertyNames pattern`() {
    // when
    val objectDataType = getDataType("3.1", "object_property_names.yaml") as ObjectDataType

    // then
    val propertyNames = objectDataType.propertyNamesDataType!!
    assert(propertyNames.validate("snake_case").isSuccess())
    assert(propertyNames.validate("CamelCase").isFailure())
  }

  @Test
  fun `rejects 3-1 schema when a declared property name violates propertyNames`() {
    // when
    val result = loadOperations("3.1", "object_property_names_violation_error.yaml")

    // then
    val errors = result.assertFailure()
    assert(errors.any { it.contains("propertyNames") }) {
      "Expected an error mentioning propertyNames violation but got: $errors"
    }
  }

  @Test
  fun `extract ObjectDataType when propertyNames is a ref to a string schema`() {
    // when
    val objectDataType = getDataType("3.1", "object_property_names_ref.yaml") as ObjectDataType

    // then
    val propertyNames = objectDataType.propertyNamesDataType!!
    assert(propertyNames.validate("snake_case").isSuccess())
    assert(propertyNames.validate("CamelCase").isFailure())
  }

  @Test
  fun `rejects 3-1 schema when propertyNames is not a string schema`() {
    // when
    val result = loadOperations("3.1", "object_property_names_non_string_error.yaml")

    // then
    val errors = result.assertFailure()
    assert(errors.any { it.contains("propertyNames") && it.contains("string") }) 
  }

  // --- Circular references ---

  @Test
  fun `extract circular reference with 3 member cycle`() {
    // when
    val result = loadOperations("3.0", "circular_reference.yaml")

    // then
    val operations = result.assertSuccess()
    val person = operations.first().requestSchema.bodies.first().dataType as ObjectDataType
    assert(person.properties.containsKey("name"))
    assert(person.properties.containsKey("address"))

    val address = person.properties["address"] as ObjectDataType
    assert(address.properties.containsKey("street"))
    assert(address.properties.containsKey("city"))

    val city = address.properties["city"] as ObjectDataType
    assert(city.properties.containsKey("name"))
    assert(city.properties.containsKey("mayor"))
  }

  @Test
  fun `extract circular reference with required nullable property`() {
    // when
    val result = loadOperations("3.0", "circular_reference_nullable.yaml")

    // then
    result.assertSuccess()
  }

  @Test
  fun `extract circular reference through array items`() {
    // when
    val result = loadOperations("3.0", "circular_reference_array.yaml")

    // then
    result.assertSuccess()
  }

  @Test
  fun `fail to load spec with infinite circular reference`() {
    // when
    val result = loadOperations("3.0", "circular_reference_infinite.yaml")

    // then
    val errors = result.assertFailure()
    assert(errors.any { it.contains("Circular") && it.contains("Node") })
  }

  @Test
  fun `fail to load spec with infinite circular reference through allOf`() {
    // when
    val result = loadOperations("3.0", "circular_reference_infinite_allof.yaml")

    // then
    val errors = result.assertFailure()
    assert(errors.any { it.contains("Circular") })
  }

  // --- Helpers ---

  private fun getDataType(version: String, yamlFile: String, propName: String = "prop1"): DataType<out Any> =
    loadOperations(version, yamlFile)
      .assertSuccess()
      .first()
      .requestSchema.bodies.first().dataType
      .asObjectDataType()
      .properties[propName]!!

  private fun loadOperations(version: String, yamlFile: String) =
    OpenApiLoader.loadOperations("src/test/resources/datatype/$version/$yamlFile")

  private fun DataType<*>.asObjectDataType() = this as ObjectDataType

  companion object {
    @JvmStatic
    fun constFixtures() = listOf(
      Arguments.of("string_const.yaml", "active", "inactive"),
      Arguments.of("integer_const.yaml", 42, 43),
      Arguments.of("number_const.yaml", 3.14, 2.71),
      Arguments.of("boolean_const.yaml", true, false),
    )
  }
}
