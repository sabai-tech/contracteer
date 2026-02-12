package tech.sabai.contracteer.core.swagger

import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.assertFailure
import tech.sabai.contracteer.core.assertSuccess
import tech.sabai.contracteer.core.datatype.*

class SchemaConversionTest {

  @Test
  fun `extract IntegerDataType`() {
    // when
    val integerDataType = getDataType("integer.yaml") as IntegerDataType

    // then
    assert(integerDataType.allowedValues != null)
    assert(integerDataType.allowedValues!!.contains(10).isSuccess())
    assert(integerDataType.allowedValues.contains(20).isSuccess())
    assert(integerDataType.range.minimum == 9.toBigDecimal())
    assert(integerDataType.range.maximum == 20.toBigDecimal())
    assert(integerDataType.range.exclusiveMinimum)
    assert(integerDataType.range.exclusiveMaximum.not())
  }

  @Test
  fun `extract NumberDataType`() {
    // when
    val numberDataType = getDataType("number.yaml") as NumberDataType

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
    val numberDataType = getDataType("number_without_type.yaml") as NumberDataType

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
  fun `extract StringDataType`() {
    // when
    val stringDataType = getDataType("string.yaml") as StringDataType

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
    val stringDataType = getDataType("string_without_type.yaml") as StringDataType

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
  fun `extract Base64DataType`() {
    // when
    val base64DataType = getDataType("string_base64.yaml") as Base64DataType

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
  fun `extract BinaryDataType`() {
    // when
    val binaryDataType = getDataType("string_binary.yaml") as BinaryDataType

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
  fun `extract UuidDataType`() {
    // when
    val uuidDataType = getDataType("string_uuid.yaml") as UuidDataType

    // then
    assert(uuidDataType.allowedValues != null)
    assert(uuidDataType.allowedValues!!.contains("d972d2c3-9b84-4076-a836-aa2465acd9fb").isSuccess())
    assert(uuidDataType.allowedValues.contains("24b12872-6410-46c5-81d3-c589e849dfca").isSuccess())
  }

  @Test
  fun `extract EmailDataType`() {
    // when
    val emailDataType = getDataType("string_email.yaml") as EmailDataType

    // then
    assert(emailDataType.allowedValues != null)
    assert(emailDataType.allowedValues!!.contains("john@example.com").isSuccess())
    assert(emailDataType.allowedValues.contains("jane@example.com").isSuccess())
    assert(emailDataType.lengthRange.minimum == 6.toBigDecimal())
    assert(emailDataType.lengthRange.maximum == 100.toBigDecimal())
    assert(emailDataType.lengthRange.exclusiveMinimum.not())
    assert(emailDataType.lengthRange.exclusiveMaximum.not())
  }

  @Test
  fun `extract DateDataType`() {
    // when
    val dateDataType = getDataType("string_date.yaml") as DateDataType

    // then
    assert(dateDataType.allowedValues != null)
    assert(dateDataType.allowedValues!!.contains("2020-12-01").isSuccess())
    assert(dateDataType.allowedValues.contains("2024-01-01").isSuccess())
  }

  @Test
  fun `extract DateTimeDataType`() {
    // when
    val dateTimeDataType = getDataType("string_datetime.yaml") as DateTimeDataType

    // then
    assert(dateTimeDataType.allowedValues != null)
    assert(dateTimeDataType.allowedValues!!.contains("2020-12-20T15:30:45+02:00").isSuccess())
    assert(dateTimeDataType.allowedValues.contains("2024-12-20T15:30:45+02:00").isSuccess())
  }

  @Test
  fun `extract BooleanDataType`() {
    // when
    val booleanDataType = getDataType("boolean.yaml") as BooleanDataType

    // then
    assert(booleanDataType.allowedValues != null)
    assert(booleanDataType.allowedValues!!.contains(false).isSuccess())
  }

  @Test
  fun `extract BooleanDataType without type property`() {
    // when
    val booleanDataType = getDataType("boolean_without_type.yaml") as BooleanDataType

    // then
    assert(booleanDataType.allowedValues != null)
    assert(booleanDataType.allowedValues!!.contains(false).isSuccess())
  }

  @Test
  fun `extract ArrayDataType`() {
    // when
    val arrayDataType = getDataType("array.yaml") as ArrayDataType

    // then
    assert(arrayDataType.itemDataType is StringDataType)
    assert(arrayDataType.allowedValues != null)
    assert(arrayDataType.allowedValues!!.contains(listOf("cat", "dog")).isSuccess())
    assert(arrayDataType.allowedValues.contains(listOf("john", "jane")).isSuccess())
  }

  @Test
  fun `extract ObjectDataType`() {
    // when
    val objectDataType = getDataType("object.yaml") as ObjectDataType

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
    val objectDataType = getDataType("object_without_type.yaml") as ObjectDataType

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
    val objectDataType = getDataType("object_additional_properties.yaml") as ObjectDataType

    // then
    assert(objectDataType.properties.keys == setOf("name", "age"))
    assert(objectDataType.properties["name"]!! is StringDataType)
    assert(objectDataType.properties["age"]!! is IntegerDataType)
    assert(objectDataType.allowAdditionalProperties)
    assert(objectDataType.additionalPropertiesDataType is StringDataType)
  }

  @Test
  fun `extract AnyOfDataType`() {
    // when
    val anyOfDataType = getDataType("anyOf.yaml") as AnyOfDataType

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
    val anyOfDataType = getDataType("anyOf_discriminator.yaml") as AnyOfDataType

    // then
    assert(anyOfDataType.subTypes.size == 2)
    assert(anyOfDataType.subTypes.all { it.name == "cat" || it.name == "dog" })
    assert(anyOfDataType.discriminator == Discriminator("type", mapOf("DOG" to "dog")))
  }

  @Test
  fun `extract OneOfDataType`() {
    // when
    val oneOfDataType = getDataType("oneOf.yaml") as OneOfDataType

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
    val oneOfDataType = getDataType("oneOf_discriminator.yaml") as OneOfDataType

    // then
    assert(oneOfDataType.subTypes.size == 2)
    assert(oneOfDataType.subTypes.all { it.name == "cat" || it.name == "dog" })
    assert(oneOfDataType.discriminator == Discriminator("type", mapOf("DOG" to "dog")))
  }

  @Test
  fun `extract AllOfDataType`() {
    // when
    val allOfDataType = getDataType("allOf.yaml") as AllOfDataType

    // then
    assert(allOfDataType.subTypes.size == 2)
    assert(allOfDataType.subTypes.all { it.name == "pet" || it.name == "allOf #0" })
    assert(allOfDataType.allowedValues!!.contains(mapOf("name" to "kitty", "age" to 3)).isSuccess())
    assert(allOfDataType.allowedValues.contains(mapOf("name" to "medor", "age" to 4)).isSuccess())
  }

  @Test
  fun `extract AllOfDataType with discriminator`() {
    // when
    val allOfDataType = getDataType("allOf_discriminator.yaml") as AllOfDataType

    // then
    assert(allOfDataType.subTypes.size == 2)
    assert(allOfDataType.subTypes.all { it.name == "Pet" || it.name == "allOf #1" })
    assert(allOfDataType.discriminator == Discriminator("petType", mapOf("dog" to "Dog")))
  }

  @Test
  fun `extract AnyDataType`() {
    // when
    val anyDataType = getDataType("any.yaml")

    // then
    assert(anyDataType is AnyDataType)
  }

  @Test
  fun `does not extract AllOfDataType when there are multiple discriminators`() {
    // when
    val result = loadOperations("allOf_multiple_discriminators_error.yaml")

    // then
    result.assertFailure()
  }

  @Test
  fun `does not extract AllOfDataType when sub datatypes are not structured`() {
    // when
    val result = loadOperations("allOf_subtypes_error.yaml")

    // then
    result.assertFailure()
  }

  @Test
  fun `extract AllOfDataType with inheritance`() {
    // when
    val operations = loadOperations("allOf_inheritance.yaml").assertSuccess()

    // then
    val bodyDataType = operations.first().requestSchema.bodies.first().dataType
    assert(bodyDataType is AnyOfDataType)
    val anyOfDataType = bodyDataType as AnyOfDataType
    assert(anyOfDataType.subTypes.size == 3)
    assert(anyOfDataType.subTypes.all { it.name == "Dog" || it.name == "Cat" || it.name == "Lizard" })
    assert(anyOfDataType.subTypes.all { it is AllOfDataType })
  }

  // --- Helpers ---

  private fun getDataType(yamlFile: String): DataType<out Any> =
    loadOperations(yamlFile)
      .assertSuccess()
      .first()
      .requestSchema.bodies.first().dataType
      .asObjectDataType()
      .properties["prop1"]!!

  private fun loadOperations(yamlFile: String) =
    OpenApiLoader.loadOperations("src/test/resources/datatype/$yamlFile")

  private fun DataType<*>.asObjectDataType() = this as ObjectDataType
}
