package tech.sabai.contracteer.core.datatype

import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.DataTypeFixture.dateTimeDataType

class DateTimeDataTypeTest {

  @Test
  fun `validates a value of type string representing a date-time`() {
    // given
    val dateTimeDateType = dateTimeDataType()

    // when
    val result = dateTimeDateType.validate("2024-12-20T15:30:45+02:00")

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate string value which does not represent a date-time`() {
    // given
    val dateTimeDateType = dateTimeDataType()

    // when
    val result = dateTimeDateType.validate("john doe")

    // then
    assert(result.isFailure())
  }
  @Test
  fun `validates null value if it is nullable`() {
    // given
    val dateTimeDateType = dateTimeDataType(isNullable = true)

    // when
    val result = dateTimeDateType.validate(null)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate null value if it is not nullable`() {
    // given
    val dateTimeDateType = dateTimeDataType(isNullable = false)

    // when
    val result = dateTimeDateType.validate(null)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `should generate a string representing a date-time`() {
    // given
    val dateTimeDateType = dateTimeDataType()

    // when
    val randomDateTime = dateTimeDateType.randomValue()

    // then
    assert(dateTimeDateType.validate(randomDateTime).isSuccess())
  }

  @Test
  fun `validates a string representing a date-time with enum values`() {
    // given
    val dateTimeDataType = dateTimeDataType(enum = listOf("2024-12-20T15:30:45+02:00", "2030-10-20T20:12:45Z"))

    // when
    val result = dateTimeDataType.validate("2030-10-20T20:12:45Z")

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate a string representing a date with enum values`() {
    // given
    val dateTimeDataType = dateTimeDataType(enum = listOf("2024-12-20T15:30:45+02:00", "2030-10-20T20:12:45Z"))

    // when
    val result = dateTimeDataType.validate("2015-12-20T15:30:45Z")

    // then
    assert(result.isFailure())
  }

  @Test
  fun `generates random value with enum values`() {
    // given
    val enum = listOf("2024-12-20T15:30:45+02:00", "2030-10-20T20:12:45Z")
    val dateTimeDataType = dateTimeDataType(enum = enum)

    // when
    val result = dateTimeDataType.randomValue()

    // then
    assert(enum.contains(result))
  }
}