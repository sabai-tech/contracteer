package tech.sabai.contracteer.core.swagger.converter

import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.datatype.*
import tech.sabai.contracteer.core.swagger.loadContracts
import kotlin.io.path.Path

class SchemaConverterTest {

  @Test
  fun `extract IntegerDataType`() {
    // when
    val contractResults = Path("src/test/resources/datatype/integer_datatype.yaml").loadContracts()
    val integerDataType =
      contractResults.value!!.first().request.body!!.dataType.asObjectDataType().properties["prop1"] as IntegerDataType

    // then
    assert(integerDataType.allowedValues != null)
    assert(integerDataType.allowedValues!!.contains(10).isSuccess())
    assert(integerDataType.allowedValues!!.contains(20).isSuccess())
    assert(integerDataType.range.minimum == 9.toBigDecimal())
    assert(integerDataType.range.maximum == 20.toBigDecimal())
    assert(integerDataType.range.exclusiveMinimum)
    assert(integerDataType.range.exclusiveMaximum.not())
  }

  @Test
  fun `extract NumberDataType`() {
    // when
    val contractResults = Path("src/test/resources/datatype/number_datatype.yaml").loadContracts()
    val numberDataType =
      contractResults.value!!.first().request.body!!.dataType.asObjectDataType().properties["prop1"] as NumberDataType

    // then
    assert(numberDataType.allowedValues != null)
    assert(numberDataType.allowedValues!!.contains(10.5).isSuccess())
    assert(numberDataType.allowedValues!!.contains(20).isSuccess())
    assert(numberDataType.range.minimum == 10.toBigDecimal())
    assert(numberDataType.range.maximum == 20.3.toBigDecimal())
    assert(numberDataType.range.exclusiveMinimum)
    assert(numberDataType.range.exclusiveMaximum.not())
  }

  @Test
  fun `extract StringDataType`() {
    // when
    val contractResults = Path("src/test/resources/datatype/string__datatype.yaml").loadContracts()
    val stringDataType =
      contractResults.value!!.first().request.body!!.dataType.asObjectDataType().properties["prop1"] as StringDataType

    // then
    assert(stringDataType.allowedValues != null)
    assert(stringDataType.allowedValues!!.contains("cat").isSuccess())
    assert(stringDataType.allowedValues!!.contains("dog").isSuccess())
    assert(stringDataType.lengthRange.minimum == 2.toBigDecimal())
    assert(stringDataType.lengthRange.maximum == 10.toBigDecimal())
    assert(stringDataType.lengthRange.exclusiveMinimum.not())
    assert(stringDataType.lengthRange.exclusiveMaximum.not())
  }

  @Test
  fun `extract Base64DataType`() {
    // when
    val contractResults = Path("src/test/resources/datatype/string_base64_datatype.yaml").loadContracts()
    val base64DataType =
      contractResults.value!!.first().request.body!!.dataType.asObjectDataType().properties["prop1"] as Base64DataType

    // then
    assert(base64DataType.allowedValues != null)
    assert(base64DataType.allowedValues!!.contains("Y2F0").isSuccess())
    assert(base64DataType.allowedValues!!.contains("ZG9n").isSuccess())
    assert(base64DataType.lengthRange.minimum == 4.toBigDecimal())
    assert(base64DataType.lengthRange.maximum == 12.toBigDecimal())
    assert(base64DataType.lengthRange.exclusiveMinimum.not())
    assert(base64DataType.lengthRange.exclusiveMaximum.not())
  }

  @Test
  fun `extract BinaryDataType`() {
    // when
    val contractResults = Path("src/test/resources/datatype/string_binary_datatype.yaml").loadContracts()
    val binaryDataType =
      contractResults.value!!.first().request.body!!.dataType.asObjectDataType().properties["prop1"] as BinaryDataType

    // then
    assert(binaryDataType.allowedValues != null)
    assert(binaryDataType.allowedValues!!.contains("ÔSÌì&").isSuccess())
    assert(binaryDataType.allowedValues!!.contains("Çþ}OZ").isSuccess())
    assert(binaryDataType.lengthRange.minimum == 2.toBigDecimal())
    assert(binaryDataType.lengthRange.maximum == 10.toBigDecimal())
    assert(binaryDataType.lengthRange.exclusiveMinimum.not())
    assert(binaryDataType.lengthRange.exclusiveMaximum.not())
  }

  @Test
  fun `extract UuidDataType`() {
    // when
    val contractResults = Path("src/test/resources/datatype/string_uuid_datatype.yaml").loadContracts()
    val uuidDataType =
      contractResults.value!!.first().request.body!!.dataType.asObjectDataType().properties["prop1"] as UuidDataType

    // then
    assert(uuidDataType.allowedValues != null)
    assert(uuidDataType.allowedValues!!.contains("d972d2c3-9b84-4076-a836-aa2465acd9fb").isSuccess())
    assert(uuidDataType.allowedValues!!.contains("24b12872-6410-46c5-81d3-c589e849dfca").isSuccess())
  }

  @Test
  fun `extract EmailDataType`() {
    // when
    val contractResults = Path("src/test/resources/datatype/string_email_datatype.yaml").loadContracts()
    val emailDataType =
      contractResults.value!!.first().request.body!!.dataType.asObjectDataType().properties["prop1"] as EmailDataType

    // then
    assert(emailDataType.allowedValues != null)
    assert(emailDataType.allowedValues!!.contains("john@example.com").isSuccess())
    assert(emailDataType.allowedValues!!.contains("jane@example.com").isSuccess())
    assert(emailDataType.lengthRange.minimum == 6.toBigDecimal())
    assert(emailDataType.lengthRange.maximum == 100.toBigDecimal())
    assert(emailDataType.lengthRange.exclusiveMinimum.not())
    assert(emailDataType.lengthRange.exclusiveMaximum.not())
  }

  @Test
  fun `extract DateDataType`() {
    // when
    val contractResults = Path("src/test/resources/datatype/string_date_datatype.yaml").loadContracts()
    val dateDataType =
      contractResults.value!!.first().request.body!!.dataType.asObjectDataType().properties["prop1"] as DateDataType

    // then
    assert(dateDataType.allowedValues != null)
    assert(dateDataType.allowedValues!!.contains("2020-12-01").isSuccess())
    assert(dateDataType.allowedValues!!.contains("2024-01-01").isSuccess())
  }
  @Test
  fun `extract DateTimeDataType`() {
    // when
    val contractResults = Path("src/test/resources/datatype/string_datetime_datatype.yaml").loadContracts()
    val dateTimeDataType =
      contractResults.value!!.first().request.body!!.dataType.asObjectDataType().properties["prop1"] as DateTimeDataType

    // then
    assert(dateTimeDataType.allowedValues != null)
    assert(dateTimeDataType.allowedValues!!.contains("2020-12-20T15:30:45+02:00").isSuccess())
    assert(dateTimeDataType.allowedValues!!.contains("2024-12-20T15:30:45+02:00").isSuccess())
  }

  private fun DataType<*>.asObjectDataType() = this as ObjectDataType
}