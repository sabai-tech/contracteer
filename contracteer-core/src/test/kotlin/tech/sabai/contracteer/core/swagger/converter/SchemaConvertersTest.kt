package tech.sabai.contracteer.core.swagger.converter

import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.contract.Contract
import tech.sabai.contracteer.core.datatype.*
import tech.sabai.contracteer.core.swagger.loadContracts
import kotlin.io.path.Path

class SchemaConvertersTest {

  @Test
  fun `extract IntegerDataType`() {
    // when
    val contractResults = Path("src/test/resources/datatype/integer_datatype.yaml").loadContracts()
    val integerDataType = getDataType(contractResults) as IntegerDataType

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
    val contractResults = Path("src/test/resources/datatype/number_datatype.yaml").loadContracts()
    val numberDataType = getDataType(contractResults) as NumberDataType

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
    val contractResults = Path("src/test/resources/datatype/string__datatype.yaml").loadContracts()
    val stringDataType = getDataType(contractResults) as StringDataType

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
    val contractResults = Path("src/test/resources/datatype/string_base64_datatype.yaml").loadContracts()
    val base64DataType = getDataType(contractResults) as Base64DataType

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
    val contractResults = Path("src/test/resources/datatype/string_binary_datatype.yaml").loadContracts()
    val binaryDataType = getDataType(contractResults) as BinaryDataType

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
    val contractResults = Path("src/test/resources/datatype/string_uuid_datatype.yaml").loadContracts()
    val uuidDataType = getDataType(contractResults) as UuidDataType

    // then
    assert(uuidDataType.allowedValues != null)
    assert(uuidDataType.allowedValues!!.contains("d972d2c3-9b84-4076-a836-aa2465acd9fb").isSuccess())
    assert(uuidDataType.allowedValues.contains("24b12872-6410-46c5-81d3-c589e849dfca").isSuccess())
  }

  @Test
  fun `extract EmailDataType`() {
    // when
    val contractResults = Path("src/test/resources/datatype/string_email_datatype.yaml").loadContracts()
    val emailDataType = getDataType(contractResults) as EmailDataType

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
    val contractResults = Path("src/test/resources/datatype/string_date_datatype.yaml").loadContracts()
    val dateDataType = getDataType(contractResults) as DateDataType

    // then
    assert(dateDataType.allowedValues != null)
    assert(dateDataType.allowedValues!!.contains("2020-12-01").isSuccess())
    assert(dateDataType.allowedValues.contains("2024-01-01").isSuccess())
  }

  @Test
  fun `extract DateTimeDataType`() {
    // when
    val contractResults = Path("src/test/resources/datatype/string_datetime_datatype.yaml").loadContracts()
    val dateTimeDataType = getDataType(contractResults) as DateTimeDataType

    // then
    assert(dateTimeDataType.allowedValues != null)
    assert(dateTimeDataType.allowedValues!!.contains("2020-12-20T15:30:45+02:00").isSuccess())
    assert(dateTimeDataType.allowedValues.contains("2024-12-20T15:30:45+02:00").isSuccess())
  }

  @Test
  fun `extract BooleanDataType`() {
    // when
    val contractResults = Path("src/test/resources/datatype/boolean_datatype.yaml").loadContracts()
    val booleanDataType = getDataType(contractResults) as BooleanDataType

    // then
    assert(booleanDataType.allowedValues != null)
    assert(booleanDataType.allowedValues!!.contains(false).isSuccess())
  }

  @Test
  fun `extract ArrayDataType`() {
    // when
    val contractResults = Path("src/test/resources/datatype/array_datatype.yaml").loadContracts()
    val arrayDataType = getDataType(contractResults) as ArrayDataType

    // then
    assert(arrayDataType.itemDataType is StringDataType)
    assert(arrayDataType.allowedValues != null)
    assert(arrayDataType.allowedValues!!.contains(listOf("cat", "dog")).isSuccess())
    assert(arrayDataType.allowedValues.contains(listOf("john", "jane")).isSuccess())
  }

  @Test
  fun `extract ObjectDataType`() {
    // when
    val contractResults = Path("src/test/resources/datatype/object_datatype.yaml").loadContracts()
    val objectDataType = getDataType(contractResults) as ObjectDataType

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
  fun `extract anyOfDataType`() {
    // when
    val contractResults = Path("src/test/resources/datatype/anyOf.yaml").loadContracts()
    val anyOfDataType = getDataType(contractResults) as AnyOfDataType

    // then
    assert(anyOfDataType.subTypes.all { it is StringDataType || it is IntegerDataType || it is ObjectDataType })
    assert(anyOfDataType.allowedValues!!.contains("Hello").isSuccess())
    assert(anyOfDataType.allowedValues.contains(42).isSuccess())
    assert(anyOfDataType.allowedValues.contains(mapOf("name" to "john", "age" to 42)).isSuccess())
    assert(anyOfDataType.allowedValues.contains(mapOf("prop1" to "Yo")).isSuccess())
  }

  @Test
  fun `extract anyOfDataType_with_discriminator`() {
    // when
    val contractResults = Path("src/test/resources/datatype/anyOf_discriminator.yaml").loadContracts()
    val anyOfDataType = getDataType(contractResults) as AnyOfDataType

    // then
    assert(anyOfDataType.subTypes.size == 2)
    assert(anyOfDataType.subTypes.all { it.name == "cat" || it.name == "dog" })
    assert(anyOfDataType.discriminator == Discriminator("type", mapOf("DOG" to "dog")))
  }

 @Test
  fun `extract oneOfDataType`() {
    // when
    val contractResults = Path("src/test/resources/datatype/oneOf.yaml").loadContracts()
    val oneOfDataType = getDataType(contractResults) as OneOfDataType

    // then
    assert(oneOfDataType.subTypes.all { it is StringDataType || it is IntegerDataType || it is ObjectDataType })
    assert(oneOfDataType.allowedValues!!.contains("Hello").isSuccess())
    assert(oneOfDataType.allowedValues.contains(42).isSuccess())
    assert(oneOfDataType.allowedValues.contains(mapOf("name" to "john", "age" to 42)).isSuccess())
    assert(oneOfDataType.allowedValues.contains(mapOf("prop1" to "Yo")).isSuccess())
  }

  @Test
  fun `extract oneOfDataType with discriminator`() {
    // when
    val contractResults = Path("src/test/resources/datatype/oneOf_discriminator.yaml").loadContracts()
    val oneOfDataType = getDataType(contractResults) as OneOfDataType

    // then
    assert(oneOfDataType.subTypes.size == 2)
    assert(oneOfDataType.subTypes.all { it.name == "cat" || it.name == "dog" })
    assert(oneOfDataType.discriminator == Discriminator("type", mapOf("DOG" to "dog")))
  }

  @Test
  fun `extract allOfDataType`() {
    // when
    val contractResults = Path("src/test/resources/datatype/allOf.yaml").loadContracts()
    val allOfDataType = getDataType(contractResults) as AllOfDataType

    // then
    assert(allOfDataType.subTypes.size == 2)
    assert(allOfDataType.subTypes.all { it.name == "pet" || it.name == "Inline Schema" } )
    assert(allOfDataType.allowedValues!!.contains(mapOf("name" to "kitty", "age" to 3 )).isSuccess())
    assert(allOfDataType.allowedValues.contains(mapOf("name" to "medor", "age" to 4)).isSuccess())
  }

  @Test
  fun `extract allOfDataType with discriminator`() {
    // when
    val contractResults = Path("src/test/resources/datatype/allOf_discriminator.yaml").loadContracts()
    val allOfDataType = getDataType(contractResults) as AllOfDataType

    // then
    assert(allOfDataType.subTypes.size == 2)
    assert(allOfDataType.subTypes.all { it.name == "Pet" || it.name == "Inline Schema" } )
    assert(allOfDataType.discriminator == Discriminator("petType", mapOf("dog" to "Dog")))
  }

  @Test
  fun `does not extract allOfDataType when there are multiple discriminators`() {
    // when
    val contractResults = Path("src/test/resources/datatype/allOf_multiple_discriminators_error.yaml").loadContracts()

    // then
    assert(contractResults.isFailure())
  }

  @Test
  fun `does not extract allOfDataType when sub datatypes are not structured`() {
    // when
    val contractResults = Path("src/test/resources/datatype/allOf_subtypes_error.yaml").loadContracts()

    // then
    assert(contractResults.isFailure())
  }

  @Test
  fun `generate contracts with allOf inheritance`() {
    // when
    val loadContracts = Path("src/test/resources/datatype/allOf_inheritance.yaml").loadContracts()
    val contracts = loadContracts.value!!
    // then
    assert(contracts.size == 1)
    assert(contracts.first().request.body!!.dataType is AnyOfDataType)
    assert((contracts.first().request.body!!.dataType as AnyOfDataType).subTypes.size == 3)
    assert((contracts.first().request.body!!.dataType as AnyOfDataType).subTypes.all { it.name == "Dog" || it.name == "Cat" || it.name == "Lizard" } )
    assert((contracts.first().request.body!!.dataType as AnyOfDataType).subTypes.all { it is AllOfDataType } )
  }

  private fun getDataType(contractResults: Result<List<Contract>>) =
    contractResults.value!!.first().request.body!!.dataType.asObjectDataType().properties["prop1"]

  private fun DataType<*>.asObjectDataType() =
    this as ObjectDataType
}
